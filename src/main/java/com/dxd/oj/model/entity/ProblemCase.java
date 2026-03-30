package com.dxd.oj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("problem_case")
public class ProblemCase implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long problemId;

    private Integer caseNo;

    private String inputData;

    private String outputData;

    private Integer score;

    private Integer isSample;

    private LocalDateTime createTime;
}
