package com.dxd.oj.model.dto.problem;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "创建题目请求")
public class ProblemCreateRequest {

    @Schema(description = "题目标题", example = "两数之和")
    @NotBlank(message = "题目标题不能为空")
    @Size(max = 200, message = "题目标题长度不能超过200位")
    private String title;

    @Schema(description = "分类ID", example = "10001")
    private Long categoryId;

    @Schema(description = "题目描述（支持Markdown）", example = "给定两个整数 a 和 b，输出它们的和。")
    @NotBlank(message = "题目描述不能为空")
    private String content;

    @Schema(description = "输入描述", example = "输入一行两个整数 a,b")
    private String inputDesc;

    @Schema(description = "输出描述", example = "输出一个整数表示和")
    private String outputDesc;

    @Schema(description = "样例输入", example = "1 2")
    private String sampleInput;

    @Schema(description = "样例输出", example = "3")
    private String sampleOutput;

    @Schema(description = "提示信息", example = "注意整数范围")
    private String hint;

    @Schema(description = "难度：1=简单，2=中等，3=困难", example = "1")
    @NotNull(message = "难度不能为空")
    @Min(value = 1, message = "难度取值不合法")
    @Max(value = 3, message = "难度取值不合法")
    private Integer difficulty;

    @Schema(description = "时间限制（毫秒）", example = "1000")
    @NotNull(message = "时间限制不能为空")
    @Min(value = 100, message = "时间限制不能小于100ms")
    @Max(value = 10000, message = "时间限制不能大于10000ms")
    private Integer timeLimit;

    @Schema(description = "内存限制（MB）", example = "128")
    @NotNull(message = "内存限制不能为空")
    @Min(value = 16, message = "内存限制不能小于16MB")
    @Max(value = 1024, message = "内存限制不能大于1024MB")
    private Integer memoryLimit;

    @Schema(description = "判题模式：ACM 或 OI", example = "ACM")
    @NotBlank(message = "判题模式不能为空")
    private String judgeMode;

    @Schema(description = "题目状态：0=草稿，1=发布，2=下线", example = "1")
    @NotNull(message = "题目状态不能为空")
    @Min(value = 0, message = "状态取值不合法")
    @Max(value = 2, message = "状态取值不合法")
    private Integer status;

    @Schema(description = "测试用例列表")
    @Valid
    private List<ProblemCaseDTO> caseList;
}
