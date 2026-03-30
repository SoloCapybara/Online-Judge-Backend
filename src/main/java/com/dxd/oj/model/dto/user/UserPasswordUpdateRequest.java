package com.dxd.oj.model.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "修改密码请求")
public class UserPasswordUpdateRequest {

    @Schema(description = "旧密码", example = "Admin@123456")
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @Schema(description = "新密码（6-64位）", example = "NewPass@123")
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "新密码长度需在6到64位之间")
    private String newPassword;

    @Schema(description = "确认新密码（需与newPassword一致）", example = "NewPass@123")
    @NotBlank(message = "确认密码不能为空")
    private String checkPassword;
}
