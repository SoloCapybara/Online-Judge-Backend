package com.dxd.oj.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dxd.oj.mapper.SysConfigMapper;
import com.dxd.oj.model.entity.SysConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SystemConfigBootstrapRunner implements ApplicationRunner {

    private final SysConfigMapper sysConfigMapper;

    @Override
    public void run(ApplicationArguments args) {
        List<ConfigSeed> seeds = List.of(
                new ConfigSeed("judge.max_concurrency", "判题最大并发任务数", "2", "INT", "JUDGE", "控制判题任务的最大并发数"),
                new ConfigSeed("judge.default_time_limit_ms", "默认运行时限(ms)", "2000", "INT", "JUDGE", "代码运行默认时间上限"),
                new ConfigSeed("judge.default_memory_limit_mb", "默认内存限制(MB)", "256", "INT", "JUDGE", "代码运行默认内存上限"),
                new ConfigSeed("user.register_enabled", "用户注册开关", "true", "BOOL", "USER", "false 时关闭普通用户注册"),
                new ConfigSeed("home.recent_submission_limit", "首页最近提交展示数", "5", "INT", "HOME", "首页最近提交最大展示条数"),
                new ConfigSeed("home.recent_contest_limit", "首页最近比赛展示数", "6", "INT", "HOME", "首页最近比赛最大展示条数")
        );

        int inserted = 0;
        for (ConfigSeed seed : seeds) {
            Long count = sysConfigMapper.selectCount(new LambdaQueryWrapper<SysConfig>()
                    .eq(SysConfig::getConfigKey, seed.key()));
            if (count != null && count > 0) {
                continue;
            }
            SysConfig config = new SysConfig();
            config.setConfigKey(seed.key());
            config.setConfigName(seed.name());
            config.setConfigValue(seed.value());
            config.setValueType(seed.valueType());
            config.setCategory(seed.category());
            config.setDescription(seed.description());
            config.setStatus(1);
            sysConfigMapper.insert(config);
            inserted++;
        }

        if (inserted > 0) {
            log.info("系统配置初始化完成，新增 {} 条默认配置", inserted);
        }
    }

    private record ConfigSeed(String key,
                              String name,
                              String value,
                              String valueType,
                              String category,
                              String description) {
    }
}
