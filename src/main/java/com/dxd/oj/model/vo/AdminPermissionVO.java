package com.dxd.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "管理员-权限点")
public class AdminPermissionVO implements Serializable {

    @Schema(description = "权限ID", example = "1001")
    private Long id;

    @Schema(description = "权限编码", example = "MENU_HOME")
    private String permissionCode;

    @Schema(description = "权限名称", example = "首页")
    private String permissionName;

    @Schema(description = "权限类型：MENU / API / BUTTON", example = "MENU")
    private String permissionType;

    @Schema(description = "资源路径", example = "/")
    private String path;

    @Schema(description = "请求方法", example = "GET")
    private String method;

    @Schema(description = "父权限ID", example = "0")
    private Long parentId;

    @Schema(description = "排序", example = "10")
    private Integer sortNo;

    @Schema(description = "状态：1启用，0停用", example = "1")
    private Integer status;

    @Schema(description = "描述")
    private String description;
}
