package com.dxd.oj.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dxd.oj.common.BaseResponse;
import com.dxd.oj.common.ResultUtils;
import com.dxd.oj.model.dto.admin.AdminSystemConfigPageQueryRequest;
import com.dxd.oj.model.dto.admin.AdminSystemConfigUpdateRequest;
import com.dxd.oj.model.vo.AdminSystemConfigVO;
import com.dxd.oj.model.vo.PageVO;
import com.dxd.oj.service.AdminSystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "管理员-系统配置")
@RestController
@RequestMapping("/admin/config")
@RequiredArgsConstructor
public class AdminSystemConfigController {

    private final AdminSystemConfigService adminSystemConfigService;

    @Operation(summary = "系统配置分页查询", description = "仅管理员可操作，支持关键字、分类、状态筛选。")
    @GetMapping("/page")
    public BaseResponse<PageVO<AdminSystemConfigVO>> pageConfigs(AdminSystemConfigPageQueryRequest queryRequest,
                                                                 HttpServletRequest request) {
        String token = extractToken(request);
        IPage<AdminSystemConfigVO> pageResult = adminSystemConfigService.pageConfigs(queryRequest, token);
        return ResultUtils.success(PageVO.of(pageResult));
    }

    @Operation(summary = "系统配置分类列表", description = "仅管理员可操作，返回去重后的分类选项。")
    @GetMapping("/categories")
    public BaseResponse<List<String>> listCategories(HttpServletRequest request) {
        String token = extractToken(request);
        return ResultUtils.success(adminSystemConfigService.listCategories(token));
    }

    @Operation(summary = "更新系统配置", description = "仅管理员可操作。可更新配置值、状态与说明。")
    @PutMapping("/{id}")
    public BaseResponse<Boolean> updateConfig(@Parameter(description = "配置ID", required = true)
                                              @PathVariable("id") Long id,
                                              @Valid @RequestBody AdminSystemConfigUpdateRequest updateRequest,
                                              HttpServletRequest request) {
        String token = extractToken(request);
        boolean result = adminSystemConfigService.updateConfig(id, updateRequest, token);
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
