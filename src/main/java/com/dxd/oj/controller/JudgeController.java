package com.dxd.oj.controller;

import com.dxd.oj.common.BaseResponse;
import com.dxd.oj.common.ResultUtils;
import com.dxd.oj.model.vo.JudgeOverviewVO;
import com.dxd.oj.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
