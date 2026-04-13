package com.dxd.oj.model.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "管理员-用户分页查询参数")
public class AdminUserPageQueryRequest {

    @Schema(description = "当前页码，从1开始", example = "1")
    private long current = 1;

    @Schema(description = "每页大小，最大100", example = "10")
    private long pageSize = 10;

    @Schema(description = "关键字（用户名/姓名/学号）", example = "deng")
    private String keyword;

    @Schema(description = "角色编码：STUDENT / TEACHER / ADMIN", example = "TEACHER")
    private String roleCode;

    @Schema(description = "状态：1启用，0禁用", example = "1")
    private Integer status;
}
