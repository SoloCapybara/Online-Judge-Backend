package com.dxd.oj.model.dto.contest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "比赛成绩总览分页参数")
public class ContestScorePageQueryRequest {

    @Schema(description = "当前页码，从1开始", example = "1")
    private long current = 1;

    @Schema(description = "每页大小，最大100", example = "20")
    private long pageSize = 20;

    @Schema(description = "用户名/姓名关键词", example = "zhang")
    private String usernameKeyword;

    @Schema(description = "班级关键词（当前按学号/工号匹配）", example = "2023")
    private String classKeyword;
}
