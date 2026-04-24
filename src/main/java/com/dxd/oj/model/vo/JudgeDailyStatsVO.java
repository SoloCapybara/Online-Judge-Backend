package com.dxd.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "判题每日聚合统计")
public class JudgeDailyStatsVO implements Serializable {

    @Schema(description = "统计日期（yyyy-MM-dd）")
    private String statDate;

    @Schema(description = "当日提交量")
    private Long submitCount;

    @Schema(description = "当日通过量（ACCEPTED）")
    private Long acceptedCount;

    @Schema(description = "当日AC率（0-100，保留1位小数）")
    private Double acRate;
}
