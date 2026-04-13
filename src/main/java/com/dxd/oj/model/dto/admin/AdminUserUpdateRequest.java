package com.dxd.oj.model.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "管理员-编辑用户基础信息请求")
public class AdminUserUpdateRequest {

    @Size(max = 50, message = "真实姓名长度不能超过50")
    @Schema(description = "真实姓名", example = "邓旭东")
    private String realName;

    @Size(max = 30, message = "学号/工号长度不能超过30")
    @Schema(description = "学号/工号", example = "2251300412")
    private String studentNo;

    @Size(max = 100, message = "邮箱长度不能超过100")
    @Schema(description = "邮箱", example = "dxd@example.com")
    private String email;

    @Size(max = 20, message = "手机号长度不能超过20")
    @Schema(description = "手机号", example = "18812345678")
    private String phone;

    @Min(value = 0, message = "性别参数错误")
    @Max(value = 2, message = "性别参数错误")
    @Schema(description = "性别：0未知，1男，2女", example = "1")
    private Integer gender;
}
