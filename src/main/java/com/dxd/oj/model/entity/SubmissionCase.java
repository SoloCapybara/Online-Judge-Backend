package com.dxd.oj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("submission_case")
public class SubmissionCase implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long submissionId;

    private Integer caseNo;

    private String status;

    private Integer timeUsed;

    private Integer memoryUsed;

    private String message;

    private LocalDateTime createTime;
}
