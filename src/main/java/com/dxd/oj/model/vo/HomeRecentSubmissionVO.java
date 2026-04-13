package com.dxd.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "首页最近提交条目")
public class HomeRecentSubmissionVO implements Serializable {

    @Schema(description = "提交ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户真实姓名")
    private String userRealName;

    @Schema(description = "题目ID")
    private Long problemId;

    @Schema(description = "题目标题")
    private String problemTitle;

    @Schema(description = "语言")
    private String language;

    @Schema(description = "评测状态")
    private String status;

    @Schema(description = "耗时（ms）")
    private Integer timeUsed;

    @Schema(description = "内存（MB）")
    private Integer memoryUsed;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;
}
