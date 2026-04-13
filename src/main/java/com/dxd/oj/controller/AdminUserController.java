package com.dxd.oj.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dxd.oj.common.BaseResponse;
import com.dxd.oj.common.ResultUtils;
import com.dxd.oj.model.dto.admin.AdminUserPageQueryRequest;
import com.dxd.oj.model.dto.admin.AdminUserRolesUpdateRequest;
import com.dxd.oj.model.dto.admin.AdminUserStatusUpdateRequest;
import com.dxd.oj.model.dto.admin.AdminUserUpdateRequest;
import com.dxd.oj.model.vo.AdminRoleOptionVO;
import com.dxd.oj.model.vo.AdminUserVO;
import com.dxd.oj.model.vo.PageVO;
import com.dxd.oj.service.AdminUserService;
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

@Tag(name = "管理员-用户管理")
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "用户分页查询", description = "仅管理员可操作，支持关键字、角色、状态筛选。")
    @GetMapping("/page")
    public BaseResponse<PageVO<AdminUserVO>> pageUsers(AdminUserPageQueryRequest queryRequest,
                                                       HttpServletRequest request) {
        String token = extractToken(request);
        IPage<AdminUserVO> pageResult = adminUserService.pageUsers(queryRequest, token);
        return ResultUtils.success(PageVO.of(pageResult));
    }

    @Operation(summary = "角色选项列表", description = "仅管理员可操作，用于角色分配下拉/勾选。")
    @GetMapping("/roles")
    public BaseResponse<List<AdminRoleOptionVO>> listRoles(HttpServletRequest request) {
        String token = extractToken(request);
        List<AdminRoleOptionVO> roles = adminUserService.listRoles(token);
        return ResultUtils.success(roles);
    }

    @Operation(summary = "编辑用户基础信息", description = "仅管理员可操作。")
    @PutMapping("/{id}")
    public BaseResponse<Boolean> updateUserProfile(@Parameter(description = "用户ID", required = true)
                                                   @PathVariable("id") Long userId,
                                                   @Valid @RequestBody AdminUserUpdateRequest updateRequest,
                                                   HttpServletRequest request) {
        String token = extractToken(request);
        boolean result = adminUserService.updateUserProfile(userId, updateRequest, token);
        return ResultUtils.success(result);
    }

    @Operation(summary = "更新用户状态", description = "仅管理员可操作。")
    @PutMapping("/{id}/status")
    public BaseResponse<Boolean> updateUserStatus(@Parameter(description = "用户ID", required = true)
                                                  @PathVariable("id") Long userId,
                                                  @Valid @RequestBody AdminUserStatusUpdateRequest updateRequest,
                                                  HttpServletRequest request) {
        String token = extractToken(request);
        boolean result = adminUserService.updateUserStatus(userId, updateRequest, token);
        return ResultUtils.success(result);
    }

    @Operation(summary = "更新用户角色", description = "仅管理员可操作。")
    @PutMapping("/{id}/roles")
    public BaseResponse<Boolean> updateUserRoles(@Parameter(description = "用户ID", required = true)
                                                 @PathVariable("id") Long userId,
                                                 @Valid @RequestBody AdminUserRolesUpdateRequest updateRequest,
                                                 HttpServletRequest request) {
        String token = extractToken(request);
        boolean result = adminUserService.updateUserRoles(userId, updateRequest, token);
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
