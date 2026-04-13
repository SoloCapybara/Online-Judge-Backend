package com.dxd.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "管理员-角色选项")
public class AdminRoleOptionVO implements Serializable {

    @Schema(description = "角色编码", example = "TEACHER")
    private String roleCode;

    @Schema(description = "角色名称", example = "教师")
    private String roleName;
}
