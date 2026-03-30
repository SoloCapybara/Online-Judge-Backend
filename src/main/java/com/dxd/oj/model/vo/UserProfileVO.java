package com.dxd.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "个人中心资料")
public class UserProfileVO implements Serializable {

    @Schema(description = "用户ID", example = "2038599968272379906")
    private Long id;

    @Schema(description = "用户名", example = "admin")
    private String username;

    @Schema(description = "真实姓名", example = "邓旭东")
    private String realName;

    @Schema(description = "学号/工号", example = "2251300412")
    private String studentNo;

    @Schema(description = "邮箱", example = "student001@example.com")
    private String email;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "头像URL", example = "/uploads/avatar/2026/03/a1b2c3d4.png")
    private String avatar;

    @Schema(description = "性别：0=未知，1=男，2=女", example = "1")
    private Integer gender;

    @Schema(description = "用户状态：1=启用，0=禁用", example = "1")
    private Integer status;
}
