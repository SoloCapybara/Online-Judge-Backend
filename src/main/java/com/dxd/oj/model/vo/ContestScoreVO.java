package com.dxd.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "比赛成绩总览条目")
public class ContestScoreVO implements Serializable {

    @Schema(description = "排名")
    private Long rank;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "学号/工号")
    private String studentNo;

    @Schema(description = "通过题数")
    private Integer acCount;

    @Schema(description = "总分")
    private Integer totalScore;

    @Schema(description = "总罚时（分钟）")
    private Integer penaltyMinutes;

    @Schema(description = "提交总数")
    private Integer submitCount;
}
