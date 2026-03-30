package com.dxd.oj.model.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "更新个人资料请求")
public class UserProfileUpdateRequest {

    @Schema(description = "真实姓名", example = "邓旭东")
    @Size(max = 50, message = "真实姓名长度不能超过50位")
    private String realName;

    @Schema(description = "学号/工号", example = "2251300412")
    @Size(max = 32, message = "学号长度不能超过32位")
    private String studentNo;

    @Schema(description = "邮箱（唯一）", example = "student001@example.com")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100位")
    private String email;

    @Schema(description = "手机号（11位中国大陆手机号）", example = "13800138000")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "头像URL", example = "/uploads/avatar/2026/03/a1b2c3d4.png")
    @Size(max = 512, message = "头像地址长度不能超过512位")
    private String avatar;

    @Schema(description = "性别：0=未知，1=男，2=女", example = "1")
    @Min(value = 0, message = "性别取值不合法")
    @Max(value = 2, message = "性别取值不合法")
    private Integer gender;
}
