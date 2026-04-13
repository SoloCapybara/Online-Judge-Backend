package com.dxd.oj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("contest")
public class Contest implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String title;

    private String description;

    private String type;

    private String accessType;

    private String accessPassword;

    private Integer submitLimit;

    private String resultVisibleMode;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;

    private Long creatorId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
