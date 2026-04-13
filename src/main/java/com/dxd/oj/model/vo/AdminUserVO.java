package com.dxd.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "管理员-用户信息")
public class AdminUserVO implements Serializable {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "学号/工号")
    private String studentNo;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "性别：0未知，1男，2女")
    private Integer gender;

    @Schema(description = "状态：1启用，0禁用")
    private Integer status;

    @Schema(description = "是否内置超管账号")
    private Boolean superAdmin;

    @Schema(description = "角色编码列表")
    private List<String> roleCodes;

    @Schema(description = "角色名称列表")
    private List<String> roleNames;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
