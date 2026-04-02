package com.dxd.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "判题服务概览")
public class JudgeOverviewVO implements Serializable {

    @Schema(description = "队列任务数（WAITING/JUDGING）")
    private Long queueTaskCount;

    @Schema(description = "在线判题节点数")
    private Long onlineNodeCount;

    @Schema(description = "服务状态：NORMAL/BUSY/DOWN")
    private String serviceStatus;

    @Schema(description = "今日提交量")
    private Long todaySubmissionCount;
}
