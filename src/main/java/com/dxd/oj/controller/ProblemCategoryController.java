package com.dxd.oj.controller;

import com.dxd.oj.common.BaseResponse;
import com.dxd.oj.common.ResultUtils;
import com.dxd.oj.model.dto.problem.ProblemCategoryCreateRequest;
import com.dxd.oj.model.dto.problem.ProblemCategoryUpdateRequest;
import com.dxd.oj.model.vo.ProblemCategoryCreateVO;
import com.dxd.oj.model.vo.ProblemCategoryVO;
import com.dxd.oj.service.ProblemCategoryService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "题目分类")
@RestController
@RequestMapping("/problem-category")
@RequiredArgsConstructor
public class ProblemCategoryController {

    private final ProblemCategoryService problemCategoryService;

    @Operation(summary = "分类列表", description = "用于前端下拉框与分类管理列表。学生默认只返回启用分类。")
    @GetMapping("/list")
    public BaseResponse<List<ProblemCategoryVO>> listCategories(
            @Parameter(description = "是否只返回启用分类：true=仅启用；false=全部(仅教师/管理员)") @RequestParam(required = false) Boolean onlyEnabled,
            HttpServletRequest request) {
        String token = extractToken(request);
        List<ProblemCategoryVO> list = problemCategoryService.listCategories(onlyEnabled, token);
        return ResultUtils.success(list);
    }

    @Operation(summary = "创建分类", description = "仅教师/管理员可创建。")
    @PostMapping
    public BaseResponse<ProblemCategoryCreateVO> createCategory(@Valid @RequestBody ProblemCategoryCreateRequest request,
                                                                HttpServletRequest httpServletRequest) {
        String token = extractToken(httpServletRequest);
        Long categoryId = problemCategoryService.createCategory(request, token);
        ProblemCategoryCreateVO response = new ProblemCategoryCreateVO();
        response.setCategoryId(categoryId);
        return ResultUtils.success(response);
    }

    @Operation(summary = "更新分类", description = "仅教师/管理员可更新。")
    @PutMapping("/{id}")
    public BaseResponse<Boolean> updateCategory(@Parameter(description = "分类ID", required = true) @PathVariable("id") Long id,
                                                @Valid @RequestBody ProblemCategoryUpdateRequest request,
                                                HttpServletRequest httpServletRequest) {
        String token = extractToken(httpServletRequest);
        boolean result = problemCategoryService.updateCategory(id, request, token);
        return ResultUtils.success(result);
    }

    @Operation(summary = "删除分类(逻辑停用)", description = "仅教师/管理员可删除，删除后status置为0。")
    @DeleteMapping("/{id}")
    public BaseResponse<Boolean> deleteCategory(@Parameter(description = "分类ID", required = true) @PathVariable("id") Long id,
                                                HttpServletRequest httpServletRequest) {
        String token = extractToken(httpServletRequest);
        boolean result = problemCategoryService.deleteCategory(id, token);
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
