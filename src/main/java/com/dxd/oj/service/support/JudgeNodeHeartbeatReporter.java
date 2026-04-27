package com.dxd.oj.service.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class JudgeNodeHeartbeatReporter {

    private static final String STATUS_ONLINE = "ONLINE";
    private static final String STATUS_OFFLINE = "OFFLINE";
    private static final String CONFIG_JUDGE_MAX_CONCURRENCY = "judge.max_concurrency";

    private final JdbcTemplate jdbcTemplate;
    private final SystemConfigAccessor systemConfigAccessor;

    @Value("${spring.application.name:online-judge-backend}")
    private String serviceName;

    @Value("${server.port:8080}")
    private int servicePort;

    @Value("${oj.judge.node.host:}")
    private String configuredHost;

    @Value("${oj.judge.node.heartbeat-enabled:true}")
    private boolean heartbeatEnabled;

    @Value("${oj.judge.node.version:}")
    private String configuredVersion;

    @Value("${oj.judge.node.remark:}")
    private String configuredRemark;

    private volatile Long serviceId;
    private volatile String serviceHost;

    @PostConstruct
    public void registerOnStartup() {
        if (!heartbeatEnabled) {
            return;
        }
        try {
            upsertHeartbeat(STATUS_ONLINE);
            log.info("判题节点注册成功：serviceId={}, host={}, port={}", serviceId, serviceHost, servicePort);
        } catch (Exception e) {
            log.warn("判题节点注册失败，将在下一次心跳重试", e);
        }
    }

    @Scheduled(fixedDelayString = "${oj.judge.node.heartbeat-interval-ms:5000}")
    public void reportHeartbeat() {
        if (!heartbeatEnabled) {
            return;
        }
        try {
            upsertHeartbeat(STATUS_ONLINE);
        } catch (Exception e) {
            log.warn("判题节点心跳上报失败", e);
        }
    }

    @PreDestroy
    public void markOfflineOnShutdown() {
        if (!heartbeatEnabled) {
            return;
        }
        try {
            if (serviceId == null) {
                return;
            }
            jdbcTemplate.update(
                    """
                            update judge_service
                            set service_status = ?, last_heartbeat_time = ?
                            where id = ?
                            """,
                    STATUS_OFFLINE,
                    LocalDateTime.now(),
                    serviceId
            );
        } catch (Exception e) {
            log.warn("判题节点离线标记失败", e);
        }
    }

    private synchronized void upsertHeartbeat(String status) {
        serviceHost = resolveServiceHost();
        Long existingId = findExistingServiceId(serviceName, serviceHost, servicePort);
        int maxTaskCount = currentMaxTaskCount();
        String version = resolveVersion();
        String remark = StringUtils.hasText(configuredRemark) ? configuredRemark.trim() : "judge-node-heartbeat";
        LocalDateTime now = LocalDateTime.now();
        if (existingId != null) {
            serviceId = existingId;
            jdbcTemplate.update(
                    """
                            update judge_service
                            set service_status = ?,
                                current_task_count = 0,
                                max_task_count = ?,
                                last_heartbeat_time = ?,
                                version = ?,
                                remark = ?
                            where id = ?
                            """,
                    status,
                    maxTaskCount,
                    now,
                    version,
                    remark,
                    existingId
            );
            return;
        }

        long newId = nextServiceId();
        jdbcTemplate.update(
                """
                        insert into judge_service
                        (id, service_name, service_host, service_port, service_status, current_task_count, max_task_count,
                         last_heartbeat_time, version, remark)
                        values (?, ?, ?, ?, ?, 0, ?, ?, ?, ?)
                        """,
                newId,
                serviceName,
                serviceHost,
                servicePort,
                status,
                maxTaskCount,
                now,
                version,
                remark
        );
        serviceId = newId;
    }

    private Long findExistingServiceId(String name, String host, int port) {
        return jdbcTemplate.query(
                """
                        select id
                        from judge_service
                        where service_name = ? and service_host = ? and service_port = ?
                        order by update_time desc
                        limit 1
                        """,
                rs -> rs.next() ? rs.getLong("id") : null,
                name,
                host,
                port
        );
    }

    private int currentMaxTaskCount() {
        return systemConfigAccessor.getInt(CONFIG_JUDGE_MAX_CONCURRENCY, 2, 1, 64);
    }

    private String resolveVersion() {
        if (StringUtils.hasText(configuredVersion)) {
            return configuredVersion.trim();
        }
        String implVersion = JudgeNodeHeartbeatReporter.class.getPackage().getImplementationVersion();
        return StringUtils.hasText(implVersion) ? implVersion.trim() : "dev";
    }

    private String resolveServiceHost() {
        if (StringUtils.hasText(configuredHost)) {
            return configuredHost.trim();
        }
        try {
            String host = InetAddress.getLocalHost().getHostAddress();
            if (StringUtils.hasText(host)) {
                return host;
            }
        } catch (Exception ignored) {
        }
        return "127.0.0.1";
    }

    private long nextServiceId() {
        return Math.abs(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE - 1)) + 1;
    }
}
