package com.dxd.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "提交测试点结果")
public class SubmissionCaseVO implements Serializable {

    @Schema(description = "测试点序号")
    private Integer caseNo;

    @Schema(description = "测试点状态")
    private String status;

    @Schema(description = "耗时（ms）")
    private Integer timeUsed;

    @Schema(description = "内存（MB）")
    private Integer memoryUsed;

    @Schema(description = "结果说明")
    private String message;
}
