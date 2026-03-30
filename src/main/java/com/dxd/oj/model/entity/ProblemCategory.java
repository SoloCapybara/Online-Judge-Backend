package com.dxd.oj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("problem_category")
public class ProblemCategory implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String categoryName;

    private Long parentId;

    private Integer sortNo;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
