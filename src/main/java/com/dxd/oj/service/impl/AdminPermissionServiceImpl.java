package com.dxd.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dxd.oj.common.ErrorCode;
import com.dxd.oj.exception.BusinessException;
import com.dxd.oj.mapper.SysPermissionMapper;
import com.dxd.oj.mapper.SysRoleMapper;
import com.dxd.oj.mapper.SysRolePermissionMapper;
import com.dxd.oj.model.dto.admin.AdminRolePermissionUpdateRequest;
import com.dxd.oj.model.entity.SysPermission;
import com.dxd.oj.model.entity.SysRole;
import com.dxd.oj.model.entity.SysRolePermission;
import com.dxd.oj.model.vo.AdminPermissionVO;
import com.dxd.oj.model.vo.AdminRolePermissionVO;
import com.dxd.oj.model.vo.LoginUserVO;
import com.dxd.oj.service.AdminPermissionService;
import com.dxd.oj.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminPermissionServiceImpl implements AdminPermissionService {

    private static final String ROLE_ADMIN = "ADMIN";

    private final SysPermissionMapper sysPermissionMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final AuthService authService;

    @Override
    public List<AdminPermissionVO> listPermissions(String token) {
        requireAdmin(token);
        List<SysPermission> permissionList = sysPermissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .orderByAsc(SysPermission::getPermissionType, SysPermission::getSortNo, SysPermission::getId));
        if (CollectionUtils.isEmpty(permissionList)) {
            return List.of();
        }
        return permissionList.stream().map(this::toPermissionVO).toList();
    }

    @Override
    public List<AdminRolePermissionVO> listRolePermissions(String token) {
        requireAdmin(token);
        List<SysRole> roleList = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .orderByAsc(SysRole::getId));
        if (CollectionUtils.isEmpty(roleList)) {
            return List.of();
        }

        List<SysRolePermission> rolePermissions = sysRolePermissionMapper.selectList(new LambdaQueryWrapper<SysRolePermission>()
                .in(SysRolePermission::getRoleId, roleList.stream().map(SysRole::getId).toList()));
        Map<Long, List<Long>> rolePermissionIdMap = new LinkedHashMap<>();
        if (!CollectionUtils.isEmpty(rolePermissions)) {
            for (SysRolePermission relation : rolePermissions) {
                if (relation.getRoleId() == null || relation.getPermissionId() == null) {
                    continue;
                }
                rolePermissionIdMap.computeIfAbsent(relation.getRoleId(), key -> new ArrayList<>())
                        .add(relation.getPermissionId());
            }
        }

        Set<Long> permissionIds = new LinkedHashSet<>();
        rolePermissionIdMap.values().forEach(permissionIds::addAll);
        Map<Long, SysPermission> permissionMap = new LinkedHashMap<>();
        if (!CollectionUtils.isEmpty(permissionIds)) {
            List<SysPermission> permissionList = sysPermissionMapper.selectBatchIds(permissionIds);
            if (!CollectionUtils.isEmpty(permissionList)) {
                permissionList.forEach(permission -> permissionMap.put(permission.getId(), permission));
            }
        }

        List<AdminRolePermissionVO> result = new ArrayList<>();
        for (SysRole role : roleList) {
            AdminRolePermissionVO item = new AdminRolePermissionVO();
            item.setRoleCode(role.getRoleCode());
            item.setRoleName(role.getRoleName());
            List<Long> currentPermissionIds = rolePermissionIdMap.getOrDefault(role.getId(), List.of());
            List<String> permissionCodes = currentPermissionIds.stream()
                    .map(permissionMap::get)
                    .filter(Objects::nonNull)
                    .map(SysPermission::getPermissionCode)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .toList();
            item.setPermissionCodes(permissionCodes);
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRolePermissions(AdminRolePermissionUpdateRequest request, String token) {
        requireAdmin(token);
        if (request == null || !StringUtils.hasText(request.getRoleCode()) || CollectionUtils.isEmpty(request.getPermissionCodes())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        String roleCode = request.getRoleCode().trim().toUpperCase();
        if (ROLE_ADMIN.equals(roleCode)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "ADMIN 角色权限为系统内置，不允许修改");
        }
        SysRole role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, roleCode)
                .last("limit 1"));
        if (role == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "角色不存在");
        }

        List<String> normalizedPermissionCodes = request.getPermissionCodes().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(String::toUpperCase)
                .distinct()
                .toList();
        if (normalizedPermissionCodes.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "权限编码列表不能为空");
        }

        List<SysPermission> permissions = sysPermissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .in(SysPermission::getPermissionCode, normalizedPermissionCodes));
        if (permissions.size() != normalizedPermissionCodes.size()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "存在无效权限编码");
        }

        sysRolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, role.getId()));

        for (SysPermission permission : permissions) {
            SysRolePermission relation = new SysRolePermission();
            relation.setRoleId(role.getId());
            relation.setPermissionId(permission.getId());
            sysRolePermissionMapper.insert(relation);
        }
        return true;
    }

    private AdminPermissionVO toPermissionVO(SysPermission permission) {
        AdminPermissionVO vo = new AdminPermissionVO();
        vo.setId(permission.getId());
        vo.setPermissionCode(permission.getPermissionCode());
        vo.setPermissionName(permission.getPermissionName());
        vo.setPermissionType(permission.getPermissionType());
        vo.setPath(permission.getPath());
        vo.setMethod(permission.getMethod());
        vo.setParentId(permission.getParentId());
        vo.setSortNo(permission.getSortNo());
        vo.setStatus(permission.getStatus());
        vo.setDescription(permission.getDescription());
        return vo;
    }

    private LoginUserVO requireAdmin(String token) {
        LoginUserVO loginUser = authService.getLoginUser(token);
        if (loginUser == null || CollectionUtils.isEmpty(loginUser.getRoles()) || !loginUser.getRoles().contains(ROLE_ADMIN)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅管理员可操作");
        }
        return loginUser;
    }
}
