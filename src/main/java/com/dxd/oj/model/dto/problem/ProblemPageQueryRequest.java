package com.dxd.oj.model.dto.problem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "题目分页查询参数")
public class ProblemPageQueryRequest {

    @Schema(description = "当前页码，从1开始", example = "1")
    private long current = 1;

    @Schema(description = "每页大小，最大100", example = "10")
    private long pageSize = 10;

    @Schema(description = "题目标题（模糊查询）", example = "两数之和")
    private String title;

    @Schema(description = "分类ID", example = "10001")
    private Long categoryId;

    @Schema(description = "难度：1=简单，2=中等，3=困难", example = "2")
    private Integer difficulty;

    @Schema(description = "状态：0=草稿，1=发布，2=下线（学生端忽略该参数，仅可查看发布题）", example = "1")
    private Integer status;
}
