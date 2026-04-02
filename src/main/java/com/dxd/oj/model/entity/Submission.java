package com.dxd.oj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("submission")
public class Submission implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long problemId;

    private Long contestId;

    private String language;

    private String sourceCode;

    private String status;

    private Integer timeUsed;

    private Integer memoryUsed;

    private Integer score;

    private String errorMessage;

    private LocalDateTime submitTime;

    private LocalDateTime judgeTime;
}
