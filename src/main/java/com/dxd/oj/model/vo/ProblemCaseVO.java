package com.dxd.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "题目测试用例")
public class ProblemCaseVO implements Serializable {

    @Schema(description = "测试用例ID")
    private Long id;

    @Schema(description = "测试用例序号")
    private Integer caseNo;

    @Schema(description = "输入数据")
    private String inputData;

    @Schema(description = "期望输出")
    private String outputData;

    @Schema(description = "分值")
    private Integer score;

    @Schema(description = "是否样例：0=否，1=是")
    private Integer isSample;
}
