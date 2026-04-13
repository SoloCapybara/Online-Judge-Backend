package com.dxd.oj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("contest_user")
public class ContestUser implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long contestId;

    private Long userId;

    private Integer totalScore;

    private Integer acCount;

    private Integer rankNo;

    private LocalDateTime joinTime;

    private LocalDateTime finishTime;

    private LocalDateTime createTime;
}
