package com.dxd.oj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("contest_problem")
public class ContestProblem implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long contestId;

    private Long problemId;

    private Integer displayOrder;

    private Integer score;

    private LocalDateTime createTime;
}
