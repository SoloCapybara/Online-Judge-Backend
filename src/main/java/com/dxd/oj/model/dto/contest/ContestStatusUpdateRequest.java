package com.dxd.oj.model.dto.contest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "比赛状态变更请求")
public class ContestStatusUpdateRequest {

    @Schema(description = "目标状态：0未发布，1已发布，3已关闭", example = "1")
    @NotNull(message = "status 不能为空")
    private Integer status;
}
