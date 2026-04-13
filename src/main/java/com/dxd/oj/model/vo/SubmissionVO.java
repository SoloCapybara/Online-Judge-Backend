package com.dxd.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "提交记录信息")
public class SubmissionVO implements Serializable {

    @Schema(description = "提交ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户真实姓名")
    private String userRealName;

    @Schema(description = "题目ID")
    private Long problemId;

    @Schema(description = "题目标题")
    private String problemTitle;

    @Schema(description = "比赛/作业ID")
    private Long contestId;

    @Schema(description = "比赛/作业标题")
    private String contestTitle;

    @Schema(description = "比赛/作业类型：CONTEST / HOMEWORK")
    private String contestType;

    @Schema(description = "语言")
    private String language;

    @Schema(description = "提交状态")
    private String status;

    @Schema(description = "耗时（ms）")
    private Integer timeUsed;

    @Schema(description = "内存（MB）")
    private Integer memoryUsed;

    @Schema(description = "得分")
    private Integer score;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;

    @Schema(description = "判题完成时间")
    private LocalDateTime judgeTime;
}
