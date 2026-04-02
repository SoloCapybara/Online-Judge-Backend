package com.dxd.oj.model.dto.run;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "自测运行请求")
public class RunCodeRequest {

    @Schema(description = "编程语言：JAVA17 / PYTHON3142 / C17 / CPP17", example = "JAVA17")
    @NotBlank(message = "language不能为空")
    private String language;

    @Schema(description = "源代码", example = "public class Main { public static void main(String[] args){ System.out.println(\"Hello\"); }}")
    @NotBlank(message = "sourceCode不能为空")
    @Size(max = 200000, message = "源码长度不能超过200000字符")
    private String sourceCode;

    @Schema(description = "标准输入内容（可为空）", example = "1 2")
    private String inputData;

    @Schema(description = "运行超时时间（毫秒，可不传）", example = "3000")
    private Integer timeLimitMs;
}
