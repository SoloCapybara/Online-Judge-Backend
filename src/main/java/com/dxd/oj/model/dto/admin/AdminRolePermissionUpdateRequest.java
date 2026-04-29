package com.dxd.oj.model.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "管理员-更新角色权限请求")
public class AdminRolePermissionUpdateRequest implements Serializable {

    @NotBlank(message = "角色编码不能为空")
    @Schema(description = "角色编码", example = "TEACHER", requiredMode = Schema.RequiredMode.REQUIRED)
    private String roleCode;

    @NotEmpty(message = "权限编码列表不能为空")
    @Schema(description = "权限编码列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> permissionCodes;
}
