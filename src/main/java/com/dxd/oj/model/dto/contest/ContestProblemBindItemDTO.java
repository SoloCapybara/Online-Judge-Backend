package com.dxd.oj.model.dto.contest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "比赛绑定题目信息")
public class ContestProblemBindItemDTO {

    @Schema(description = "题目ID", example = "10001")
    @NotNull(message = "problemId 不能为空")
    private Long problemId;

    @Schema(description = "显示顺序，越小越靠前", example = "1")
    private Integer displayOrder;

    @Schema(description = "题目分值", example = "100")
    private Integer score;
}
