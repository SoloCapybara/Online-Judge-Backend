package com.dxd.oj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_config")
public class SysConfig implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String configKey;

    private String configName;

    private String configValue;

    private String valueType;

    private String category;

    private String description;

    private Integer status;

    private Long updateBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
