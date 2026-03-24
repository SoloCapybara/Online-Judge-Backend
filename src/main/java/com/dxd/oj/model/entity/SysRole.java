package com.dxd.oj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_role")
public class SysRole implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String roleCode;

    private String roleName;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
