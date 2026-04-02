package com.dxd.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "提交创建结果")
public class SubmissionCreateVO implements Serializable {

    @Schema(description = "提交ID")
    private Long submissionId;

    @Schema(description = "提交当前状态")
    private String status;
}
