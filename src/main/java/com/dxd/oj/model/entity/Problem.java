package com.dxd.oj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("problem")
public class Problem implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String title;

    private Long categoryId;

    private String content;

    private String inputDesc;

    private String outputDesc;

    private String sampleInput;

    private String sampleOutput;

    private String hint;

    private Integer difficulty;

    private Integer timeLimit;

    private Integer memoryLimit;

    private String judgeMode;

    private Integer submitCount;

    private Integer acceptCount;

    private Integer status;

    private Long creatorId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
