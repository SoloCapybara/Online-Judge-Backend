package com.dxd.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "管理员-角色权限")
public class AdminRolePermissionVO implements Serializable {

    @Schema(description = "角色编码", example = "ADMIN")
    private String roleCode;

    @Schema(description = "角色名称", example = "管理员")
    private String roleName;

    @Schema(description = "权限编码列表")
    private List<String> permissionCodes;
}
