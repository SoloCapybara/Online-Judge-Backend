package com.dxd.oj.controller;

import com.dxd.oj.common.BaseResponse;
import com.dxd.oj.common.ResultUtils;
import com.dxd.oj.model.vo.JudgeDailyStatsVO;
import com.dxd.oj.model.vo.JudgeOverviewVO;
import com.dxd.oj.model.vo.JudgeSystemMetricsVO;
import com.dxd.oj.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "判题服务")
@RestController
@RequestMapping("/judge")
@RequiredArgsConstructor
public class JudgeController {

    private final SubmissionService submissionService;

    @Operation(summary = "判题服务概览", description = "用于评测状态页顶部统计卡片展示。")
    @GetMapping("/overview")
    public BaseResponse<JudgeOverviewVO> getOverview() {
        JudgeOverviewVO overview = submissionService.getJudgeOverview();
        return ResultUtils.success(overview);
    }

    @Operation(summary = "判题每日聚合趋势", description = "返回最近N天的提交量、通过量和AC率，用于监控趋势图。")
    @GetMapping("/daily-stats")
    public BaseResponse<List<JudgeDailyStatsVO>> getDailyStats(
            @Parameter(description = "统计天数，默认7，范围1-30")
            @RequestParam(value = "days", required = false) Integer days
    ) {
        List<JudgeDailyStatsVO> stats = submissionService.getJudgeDailyStats(days);
        return ResultUtils.success(stats);
    }

    @Operation(summary = "判题系统资源监控", description = "返回当前宿主机和JVM进程资源使用情况，用于系统监控面板。")
    @GetMapping("/system-metrics")
    public BaseResponse<JudgeSystemMetricsVO> getSystemMetrics() {
        JudgeSystemMetricsVO metrics = submissionService.getJudgeSystemMetrics();
        return ResultUtils.success(metrics);
    }
}
