package com.dxd.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "用户中心统计信息")
public class UserProfileStatsVO implements Serializable {

    @Schema(description = "已通过题目数（去重）", example = "32")
    private Long solvedCount;

    @Schema(description = "总提交次数", example = "158")
    private Long submitCount;

    @Schema(description = "通过提交次数", example = "64")
    private Long acceptedCount;

    @Schema(description = "AC率（百分比）", example = "40.51")
    private Double acRate;

    @Schema(description = "当前连续打卡天数", example = "4")
    private Integer currentStreakDays;

    @Schema(description = "历史最长连续打卡天数", example = "9")
    private Integer longestStreakDays;
}
