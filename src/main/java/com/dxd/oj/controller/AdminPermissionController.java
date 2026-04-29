package com.dxd.oj.controller;

import com.dxd.oj.common.BaseResponse;
import com.dxd.oj.common.ResultUtils;
import com.dxd.oj.model.dto.admin.AdminRolePermissionUpdateRequest;
import com.dxd.oj.model.vo.AdminPermissionVO;
import com.dxd.oj.model.vo.AdminRolePermissionVO;
import com.dxd.oj.service.AdminPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "管理员-权限中心")
@RestController
@RequestMapping("/admin/permissions")
@RequiredArgsConstructor
public class AdminPermissionController {

    private final AdminPermissionService adminPermissionService;

    @Operation(summary = "权限点列表", description = "仅管理员可操作。返回权限中心所需的全部权限点。")
    @GetMapping
    public BaseResponse<List<AdminPermissionVO>> listPermissions(HttpServletRequest request) {
        String token = extractToken(request);
        return ResultUtils.success(adminPermissionService.listPermissions(token));
    }

    @Operation(summary = "角色权限列表", description = "仅管理员可操作。返回每个角色的已授权权限编码。")
    @GetMapping("/roles")
    public BaseResponse<List<AdminRolePermissionVO>> listRolePermissions(HttpServletRequest request) {
        String token = extractToken(request);
        return ResultUtils.success(adminPermissionService.listRolePermissions(token));
    }

    @Operation(summary = "更新角色权限", description = "仅管理员可操作。提交角色编码与权限编码列表，覆盖原授权。")
    @PutMapping("/roles")
    public BaseResponse<Boolean> updateRolePermissions(@Valid @RequestBody AdminRolePermissionUpdateRequest updateRequest,
                                                       HttpServletRequest request) {
        String token = extractToken(request);
        return ResultUtils.success(adminPermissionService.updateRolePermissions(updateRequest, token));
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
