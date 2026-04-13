package com.dxd.oj.model.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "管理员-更新用户状态请求")
public class AdminUserStatusUpdateRequest {

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态参数错误")
    @Max(value = 1, message = "状态参数错误")
    @Schema(description = "状态：1启用，0禁用", example = "0")
    private Integer status;
}
