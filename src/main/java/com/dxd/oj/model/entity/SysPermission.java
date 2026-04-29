package com.dxd.oj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_permission")
public class SysPermission implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String permissionCode;

    private String permissionName;

    private String permissionType;

    private String path;

    private String method;

    private Long parentId;

    private Integer sortNo;

    private Integer status;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
