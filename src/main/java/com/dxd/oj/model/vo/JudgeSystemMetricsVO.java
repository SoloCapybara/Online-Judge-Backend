package com.dxd.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "判题系统资源监控")
public class JudgeSystemMetricsVO implements Serializable {

    @Schema(description = "主机系统名称")
    private String osName;

    @Schema(description = "主机系统架构")
    private String osArch;

    @Schema(description = "系统CPU使用率（0-100）")
    private Double systemCpuUsage;

    @Schema(description = "进程CPU使用率（0-100）")
    private Double processCpuUsage;

    @Schema(description = "系统平均负载")
    private Double systemLoadAverage;

    @Schema(description = "系统总内存（字节）")
    private Long systemMemoryTotal;

    @Schema(description = "系统可用内存（字节）")
    private Long systemMemoryFree;

    @Schema(description = "JVM堆总内存（字节）")
    private Long jvmHeapTotal;

    @Schema(description = "JVM堆已使用内存（字节）")
    private Long jvmHeapUsed;

    @Schema(description = "磁盘总容量（字节）")
    private Long diskTotal;

    @Schema(description = "磁盘可用容量（字节）")
    private Long diskUsable;

    @Schema(description = "JVM运行时长（毫秒）")
    private Long processUptimeMs;

    @Schema(description = "是否运行在容器环境中")
    private Boolean containerized;

    @Schema(description = "运行环境标签（HOST / CONTAINER）")
    private String runtimeEnvironment;

    @Schema(description = "容器线索（例如 docker / kubepods / containerd）")
    private String containerHint;

    @Schema(description = "采集时间（yyyy-MM-dd HH:mm:ss）")
    private String collectedAt;
}
