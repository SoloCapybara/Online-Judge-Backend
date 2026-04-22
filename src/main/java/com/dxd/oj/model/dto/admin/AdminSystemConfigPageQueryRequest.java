package com.dxd.oj.model.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "管理员-系统配置分页查询请求")
public class AdminSystemConfigPageQueryRequest {

    @Schema(description = "页码，从1开始", example = "1")
    private long current = 1;

    @Schema(description = "每页大小，最大100", example = "10")
    private long pageSize = 10;

    @Schema(description = "关键字（配置键/名称/说明）", example = "judge")
    private String keyword;

    @Schema(description = "配置分类", example = "JUDGE")
    private String category;

    @Schema(description = "状态：1启用，0停用")
    private Integer status;
}
