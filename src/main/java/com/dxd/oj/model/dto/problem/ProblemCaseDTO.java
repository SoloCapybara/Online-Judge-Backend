package com.dxd.oj.model.dto.problem;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "题目测试用例")
public class ProblemCaseDTO {

    @Schema(description = "测试用例序号，不传则按顺序自动生成", example = "1")
    private Integer caseNo;

    @Schema(description = "输入数据", example = "1 2")
    @NotBlank(message = "测试用例输入不能为空")
    private String inputData;

    @Schema(description = "期望输出", example = "3")
    @NotBlank(message = "测试用例输出不能为空")
    private String outputData;

    @Schema(description = "该测试用例分值（OI模式可用）", example = "20")
    @Min(value = 0, message = "用例分值不能小于0")
    @Max(value = 100, message = "用例分值不能大于100")
    private Integer score;

    @Schema(description = "是否样例：0=否，1=是", example = "1")
    @Min(value = 0, message = "isSample取值不合法")
    @Max(value = 1, message = "isSample取值不合法")
    private Integer isSample;
}
