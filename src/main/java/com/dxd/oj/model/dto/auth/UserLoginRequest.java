package com.dxd.oj.model.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "用户登录请求")
public class UserLoginRequest {

    @Schema(description = "登录账号（支持用户名或邮箱）", example = "admin")
    @NotBlank(message = "用户名或邮箱不能为空")
    private String username;

    @Schema(description = "登录密码", example = "Admin@123456")
    @NotBlank(message = "密码不能为空")
    private String password;
}
