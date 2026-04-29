package com.dxd.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "登录用户信息")
public class LoginUserVO implements Serializable {

    @Schema(description = "用户ID", example = "2038599968272379906")
    private Long id;

    @Schema(description = "用户名", example = "admin")
    private String username;

    @Schema(description = "真实姓名", example = "System Admin")
    private String realName;

    @Schema(description = "头像URL", example = "/uploads/avatar/2026/03/a1b2c3d4.png")
    private String avatar;

    @Schema(description = "用户状态：1=启用，0=禁用", example = "1")
    private Integer status;

    @Schema(description = "角色编码列表，例如 [ADMIN] / [TEACHER] / [STUDENT]")
    private List<String> roles;

    @Schema(description = "权限编码列表，例如 [MENU_HOME, API_PROBLEM_READ]")
    private List<String> permissionCodes;
}
