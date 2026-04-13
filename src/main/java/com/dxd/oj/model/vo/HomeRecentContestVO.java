package com.dxd.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "首页最近比赛条目")
public class HomeRecentContestVO implements Serializable {

    @Schema(description = "比赛ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "类型：CONTEST / HOMEWORK")
    private String type;

    @Schema(description = "参与方式：PUBLIC / PRIVATE")
    private String accessType;

    @Schema(description = "状态文本")
    private String statusText;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;
}
