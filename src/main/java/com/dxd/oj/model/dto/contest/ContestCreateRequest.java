package com.dxd.oj.model.dto.contest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "创建比赛/作业请求")
public class ContestCreateRequest {

    @Schema(description = "标题", example = "程序设计周赛 No.1")
    @NotBlank(message = "标题不能为空")
    private String title;

    @Schema(description = "描述", example = "用于课程周赛训练")
    private String description;

    @Schema(description = "类型：CONTEST / HOMEWORK", example = "CONTEST")
    @NotBlank(message = "类型不能为空")
    private String type;

    @Schema(description = "参与方式：PUBLIC / PRIVATE", example = "PUBLIC")
    private String accessType;

    @Schema(description = "访问密码（PRIVATE 时可选）", example = "123456")
    private String accessPassword;

    @Schema(description = "提交次数限制，null 表示不限", example = "20")
    private Integer submitLimit;

    @Schema(description = "结果可见规则：REALTIME / AFTER_END", example = "REALTIME")
    private String resultVisibleMode;

    @Schema(description = "开始时间", example = "2026-05-15 19:00:00")
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @Schema(description = "结束时间", example = "2026-05-15 21:00:00")
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    @Schema(description = "状态：仅支持0(未发布)，默认0", example = "0")
    private Integer status;
}
