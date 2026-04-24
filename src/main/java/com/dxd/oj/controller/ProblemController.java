package com.dxd.oj.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dxd.oj.common.BaseResponse;
import com.dxd.oj.common.ResultUtils;
import com.dxd.oj.model.dto.problem.ProblemCreateRequest;
import com.dxd.oj.model.dto.problem.ProblemPageQueryRequest;
import com.dxd.oj.model.dto.problem.ProblemUpdateRequest;
import com.dxd.oj.model.vo.PageVO;
import com.dxd.oj.model.vo.ProblemCaseVO;
import com.dxd.oj.model.vo.ProblemCreateVO;
import com.dxd.oj.model.vo.ProblemTagVO;
import com.dxd.oj.model.vo.ProblemVO;
import com.dxd.oj.service.ProblemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "题库管理")
@RestController
@RequestMapping("/problem")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;

    @Operation(summary = "题目分页查询", description = "学生仅可查询已发布题目；教师/管理员可按状态筛选全部题目。")
    @GetMapping("/page")
    public BaseResponse<PageVO<ProblemVO>> pageProblems(ProblemPageQueryRequest queryRequest, HttpServletRequest request) {
        String token = extractToken(request);
        IPage<ProblemVO> pageResult = problemService.pageProblems(queryRequest, token);
        return ResultUtils.success(PageVO.of(pageResult));
    }

    @Operation(summary = "查询题目标签列表", description = "用于题库筛选和题目编辑页标签选择。")
    @GetMapping("/tags")
    public BaseResponse<List<ProblemTagVO>> listProblemTags() {
        return ResultUtils.success(problemService.listProblemTags());
    }

    @Operation(summary = "查询题目详情", description = "学生仅可查看已发布题目详情；教师/管理员可查看全部。")
    @GetMapping("/{id}")
    public BaseResponse<ProblemVO> getProblemDetail(@Parameter(description = "题目ID", required = true)
                                                    @PathVariable("id") Long id,
                                                    HttpServletRequest request) {
        String token = extractToken(request);
        ProblemVO detail = problemService.getProblemDetail(id, token);
        return ResultUtils.success(detail);
    }

    @Operation(summary = "创建题目", description = "仅教师/管理员可创建，支持一次性提交测试用例列表。")
    @PostMapping
    public BaseResponse<ProblemCreateVO> createProblem(@Valid @RequestBody ProblemCreateRequest createRequest,
                                                       HttpServletRequest request) {
        String token = extractToken(request);
        Long problemId = problemService.createProblem(createRequest, token);
        ProblemCreateVO response = new ProblemCreateVO();
        response.setProblemId(problemId);
        return ResultUtils.success(response);
    }

    @Operation(summary = "更新题目", description = "仅题目创建者(教师)或管理员可更新；提交的caseList会覆盖原测试用例。")
    @PutMapping("/{id}")
    public BaseResponse<Boolean> updateProblem(@Parameter(description = "题目ID", required = true)
                                               @PathVariable("id") Long id,
                                               @Valid @RequestBody ProblemUpdateRequest updateRequest,
                                               HttpServletRequest request) {
        String token = extractToken(request);
        boolean result = problemService.updateProblem(id, updateRequest, token);
        return ResultUtils.success(result);
    }

    @Operation(summary = "删除题目(逻辑下线)", description = "仅题目创建者(教师)或管理员可删除，删除行为为状态改为下线。")
    @DeleteMapping("/{id}")
    public BaseResponse<Boolean> deleteProblem(@Parameter(description = "题目ID", required = true)
                                               @PathVariable("id") Long id,
                                               HttpServletRequest request) {
        String token = extractToken(request);
        boolean result = problemService.deleteProblem(id, token);
        return ResultUtils.success(result);
    }

    @Operation(summary = "查询题目测试用例", description = "仅教师/管理员可查看测试用例详情。")
    @GetMapping("/{id}/cases")
    public BaseResponse<List<ProblemCaseVO>> listProblemCases(@Parameter(description = "题目ID", required = true)
                                                              @PathVariable("id") Long id,
                                                              HttpServletRequest request) {
        String token = extractToken(request);
        List<ProblemCaseVO> cases = problemService.listProblemCases(id, token);
        return ResultUtils.success(cases);
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
