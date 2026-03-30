package com.dxd.oj.model.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
@Schema(description = "用户注册请求")
public class UserRegisterRequest {

    @Schema(description = "用户名（唯一）", example = "student001")
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "登录密码（至少6位）", example = "Abc123456")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "确认密码（需与password一致）", example = "Abc123456")
    @NotBlank(message = "确认密码不能为空")
    private String checkPassword;

    @Schema(description = "真实姓名", example = "邓旭东")
    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    @Schema(description = "邮箱（可选，唯一）", example = "student001@example.com")
    @Email(message = "邮箱格式不正确")
    private String email;
}
