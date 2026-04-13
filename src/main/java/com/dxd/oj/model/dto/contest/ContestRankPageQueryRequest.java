package com.dxd.oj.model.dto.contest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "比赛榜单分页参数")
public class ContestRankPageQueryRequest {

    @Schema(description = "当前页码，从1开始", example = "1")
    private long current = 1;

    @Schema(description = "每页大小，最大100", example = "20")
    private long pageSize = 20;
}
