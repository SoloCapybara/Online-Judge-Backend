package com.dxd.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "创建题目结果")
public class ProblemCreateVO implements Serializable {

    @Schema(description = "新创建题目ID", example = "2038888888888888888")
    private Long problemId;
}
