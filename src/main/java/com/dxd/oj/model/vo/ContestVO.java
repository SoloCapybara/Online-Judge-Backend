package com.dxd.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "比赛/作业信息")
public class ContestVO implements Serializable {

    @Schema(description = "比赛ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "类型")
    private String type;

    @Schema(description = "状态编码：0未发布，1进行中，2已结束，3已关闭")
    private Integer status;

    @Schema(description = "状态文本")
    private String statusText;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "创建人ID")
    private Long creatorId;

    @Schema(description = "创建人用户名")
    private String creatorUsername;

    @Schema(description = "创建人姓名")
    private String creatorRealName;

    @Schema(description = "参与人数")
    private Integer participantCount;

    @Schema(description = "题目数")
    private Integer problemCount;

    @Schema(description = "当前用户是否已参与")
    private Boolean joined;
}
