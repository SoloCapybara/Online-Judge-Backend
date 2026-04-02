package com.dxd.oj.model.dto.submission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "提交分页查询参数")
public class SubmissionPageQueryRequest {

    @Schema(description = "当前页码，从1开始", example = "1")
    private long current = 1;

    @Schema(description = "每页大小，最大100", example = "10")
    private long pageSize = 10;

    @Schema(description = "提交状态", example = "ACCEPTED")
    private String status;

    @Schema(description = "题目ID", example = "10001")
    private Long problemId;

    @Schema(description = "比赛/作业ID", example = "20001")
    private Long contestId;

    @Schema(description = "编程语言", example = "CPP17")
    private String language;

    @Schema(description = "用户ID（仅教师/管理员可用）", example = "30001")
    private Long userId;

    @Schema(description = "是否只看我自己（教师/管理员可用）", example = "true")
    private Boolean mine;
}
