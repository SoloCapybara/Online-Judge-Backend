package com.dxd.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "创建比赛响应")
public class ContestCreateVO implements Serializable {

    @Schema(description = "比赛ID")
    private Long contestId;
}
