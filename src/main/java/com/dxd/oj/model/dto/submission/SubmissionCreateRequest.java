package com.dxd.oj.model.dto.submission;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建提交请求")
public class SubmissionCreateRequest {

    @Schema(description = "题目ID", example = "10001")
    @NotNull(message = "problemId不能为空")
    private Long problemId;

    @Schema(description = "比赛/作业ID（可选）", example = "20001")
    private Long contestId;

    @Schema(description = "语言：JAVA17 / PYTHON3142 / C17 / CPP17", example = "CPP17")
    @NotBlank(message = "language不能为空")
    private String language;

    @Schema(description = "提交源代码", example = "#include <bits/stdc++.h>\nint main(){return 0;}")
    @NotBlank(message = "sourceCode不能为空")
    @Size(max = 200000, message = "源码长度不能超过200000字符")
    private String sourceCode;
}
