package com.dxd.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "自测运行结果")
public class RunCodeResponse implements Serializable {

    @Schema(description = "执行结果状态：SUCCESS/COMPILE_ERROR/RUNTIME_ERROR/TIME_LIMIT_EXCEEDED/UNSUPPORTED_LANGUAGE/SYSTEM_ERROR", example = "SUCCESS")
    private String status;

    @Schema(description = "进程退出码，编译或运行失败时可用于排查", example = "0")
    private Integer exitCode;

    @Schema(description = "程序标准输出")
    private String stdout;

    @Schema(description = "程序标准错误（编译错误或运行错误信息）")
    private String stderr;

    @Schema(description = "兼容字段：默认等于执行耗时（毫秒）", example = "12")
    private Long timeUsedMs;

    @Schema(description = "编译耗时（毫秒）", example = "120")
    private Long compileTimeMs;

    @Schema(description = "执行耗时（毫秒）", example = "8")
    private Long executeTimeMs;

    @Schema(description = "总耗时（毫秒，含交互等待等）", example = "26300")
    private Long totalTimeMs;
}
