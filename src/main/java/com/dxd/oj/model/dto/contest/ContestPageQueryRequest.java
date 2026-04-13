package com.dxd.oj.model.dto.contest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "比赛/作业分页查询参数")
public class ContestPageQueryRequest {

    @Schema(description = "当前页码，从1开始", example = "1")
    private long current = 1;

    @Schema(description = "每页大小，最大100", example = "10")
    private long pageSize = 10;

    @Schema(description = "标题关键字", example = "周赛")
    private String title;

    @Schema(description = "类型：CONTEST / HOMEWORK", example = "CONTEST")
    private String type;

    @Schema(description = "状态：0未发布，1进行中，2已结束，3已关闭", example = "1")
    private Integer status;

    @Schema(description = "仅看我创建的（教师/管理员）", example = "true")
    private Boolean mine;
}
