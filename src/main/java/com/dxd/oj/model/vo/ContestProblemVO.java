package com.dxd.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "比赛题目信息")
public class ContestProblemVO implements Serializable {

    @Schema(description = "题目ID")
    private Long problemId;

    @Schema(description = "题目标题")
    private String problemTitle;

    @Schema(description = "显示顺序")
    private Integer displayOrder;

    @Schema(description = "分值")
    private Integer score;
}
