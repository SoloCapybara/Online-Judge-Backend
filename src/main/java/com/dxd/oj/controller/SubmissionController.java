package com.dxd.oj.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dxd.oj.common.BaseResponse;
import com.dxd.oj.common.ResultUtils;
import com.dxd.oj.model.dto.submission.SubmissionCreateRequest;
import com.dxd.oj.model.dto.submission.SubmissionPageQueryRequest;
import com.dxd.oj.model.vo.PageVO;
import com.dxd.oj.model.vo.SubmissionCaseVO;
import com.dxd.oj.model.vo.SubmissionCreateVO;
import com.dxd.oj.model.vo.SubmissionDetailVO;
import com.dxd.oj.model.vo.SubmissionVO;
import com.dxd.oj.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "提交与判题")
@RestController
@RequestMapping("/submission")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @Operation(summary = "创建提交并判题", description = "提交代码后立即执行判题并返回提交ID及最终状态。")
    @PostMapping
    public BaseResponse<SubmissionCreateVO> createSubmission(@Valid @RequestBody SubmissionCreateRequest request,
                                                             HttpServletRequest httpServletRequest) {
        String token = extractToken(httpServletRequest);
        SubmissionCreateVO response = submissionService.createSubmission(request, token);
        return ResultUtils.success(response);
    }

    @Operation(summary = "提交分页查询", description = "学生默认仅可查看本人提交；教师/管理员可按用户筛选。")
    @GetMapping("/page")
    public BaseResponse<PageVO<SubmissionVO>> pageSubmissions(SubmissionPageQueryRequest queryRequest,
                                                              HttpServletRequest httpServletRequest) {
        String token = extractToken(httpServletRequest);
        IPage<SubmissionVO> pageResult = submissionService.pageSubmissions(queryRequest, token);
        return ResultUtils.success(PageVO.of(pageResult));
    }

    @Operation(summary = "提交详情", description = "返回提交详情和源代码。")
    @GetMapping("/{id}")
    public BaseResponse<SubmissionDetailVO> getSubmissionDetail(@Parameter(description = "提交ID", required = true)
                                                                @PathVariable("id") Long id,
                                                                HttpServletRequest httpServletRequest) {
        String token = extractToken(httpServletRequest);
        SubmissionDetailVO detail = submissionService.getSubmissionDetail(id, token);
        return ResultUtils.success(detail);
    }

    @Operation(summary = "提交测试点详情", description = "返回某次提交的测试点判题明细。")
    @GetMapping("/{id}/cases")
    public BaseResponse<List<SubmissionCaseVO>> listSubmissionCases(@Parameter(description = "提交ID", required = true)
                                                                    @PathVariable("id") Long id,
                                                                    HttpServletRequest httpServletRequest) {
        String token = extractToken(httpServletRequest);
        List<SubmissionCaseVO> caseList = submissionService.listSubmissionCases(id, token);
        return ResultUtils.success(caseList);
    }

    @Operation(summary = "重判提交", description = "仅教师/管理员可重判。")
    @PostMapping("/{id}/rejudge")
    public BaseResponse<Boolean> rejudgeSubmission(@Parameter(description = "提交ID", required = true)
                                                   @PathVariable("id") Long id,
                                                   HttpServletRequest httpServletRequest) {
        String token = extractToken(httpServletRequest);
        boolean result = submissionService.rejudgeSubmission(id, token);
        return ResultUtils.success(result);
    }

    @Parameter(hidden = true)
    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
        if (authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        return authorization.trim();
    }
}
