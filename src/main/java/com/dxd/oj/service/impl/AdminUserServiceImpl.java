package com.dxd.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dxd.oj.common.ErrorCode;
import com.dxd.oj.exception.BusinessException;
import com.dxd.oj.mapper.SysRoleMapper;
import com.dxd.oj.mapper.SysUserMapper;
import com.dxd.oj.mapper.SysUserRoleMapper;
import com.dxd.oj.model.dto.admin.AdminUserPageQueryRequest;
import com.dxd.oj.model.dto.admin.AdminUserRolesUpdateRequest;
import com.dxd.oj.model.dto.admin.AdminUserStatusUpdateRequest;
import com.dxd.oj.model.dto.admin.AdminUserUpdateRequest;
import com.dxd.oj.model.entity.SysRole;
import com.dxd.oj.model.entity.SysUser;
import com.dxd.oj.model.entity.SysUserRole;
import com.dxd.oj.model.vo.AdminRoleOptionVO;
import com.dxd.oj.model.vo.AdminUserVO;
import com.dxd.oj.model.vo.LoginUserVO;
import com.dxd.oj.service.AdminUserService;
import com.dxd.oj.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private static final String SUPER_ADMIN_USERNAME = "admin";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_TEACHER = "TEACHER";
    private static final String ROLE_STUDENT = "STUDENT";

    private static final Set<String> ALLOWED_ROLE_CODES = Set.of(ROLE_STUDENT, ROLE_TEACHER, ROLE_ADMIN);
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final AuthService authService;

    @Override
    public IPage<AdminUserVO> pageUsers(AdminUserPageQueryRequest queryRequest, String token) {
        requireAdmin(token);

        AdminUserPageQueryRequest safeQuery = queryRequest == null ? new AdminUserPageQueryRequest() : queryRequest;
        long current = safeQuery.getCurrent() <= 0 ? 1 : safeQuery.getCurrent();
        long pageSize = safeQuery.getPageSize() <= 0 ? 10 : Math.min(safeQuery.getPageSize(), 100);

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        String keyword = normalizeKeyword(safeQuery.getKeyword());
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or()
                    .like(SysUser::getRealName, keyword)
                    .or()
                    .like(SysUser::getStudentNo, keyword));
        }

        Integer status = safeQuery.getStatus();
        if (status != null) {
            if (!Objects.equals(status, 0) && !Objects.equals(status, 1)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态参数错误");
            }
            wrapper.eq(SysUser::getStatus, status);
        }

        String roleCode = normalizeRoleCodeNullable(safeQuery.getRoleCode());
        if (StringUtils.hasText(roleCode)) {
            if (!ALLOWED_ROLE_CODES.contains(roleCode)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "角色参数错误");
            }
            SysRole role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getRoleCode, roleCode)
                    .last("limit 1"));
            if (role == null) {
                return emptyPage(current, pageSize);
            }

            List<SysUserRole> roleRelations = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getRoleId, role.getId()));
            if (CollectionUtils.isEmpty(roleRelations)) {
                return emptyPage(current, pageSize);
            }
            Set<Long> userIds = roleRelations.stream()
                    .map(SysUserRole::getUserId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (CollectionUtils.isEmpty(userIds)) {
                return emptyPage(current, pageSize);
            }
            wrapper.in(SysUser::getId, userIds);
        }

        wrapper.orderByDesc(SysUser::getCreateTime, SysUser::getId);
        Page<SysUser> page = sysUserMapper.selectPage(new Page<>(current, pageSize), wrapper);

        Page<AdminUserVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (CollectionUtils.isEmpty(page.getRecords())) {
            result.setRecords(List.of());
            return result;
        }

        Set<Long> userIds = page.getRecords().stream().map(SysUser::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        RoleBundle roleBundle = loadRoleBundle(userIds);
        List<AdminUserVO> records = page.getRecords().stream()
                .map(user -> toAdminUserVO(user, roleBundle))
                .toList();
        result.setRecords(records);
        return result;
    }

    @Override
    public List<AdminRoleOptionVO> listRoles(String token) {
        requireAdmin(token);
        List<SysRole> roleList = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .orderByAsc(SysRole::getId));
        if (CollectionUtils.isEmpty(roleList)) {
            return List.of();
        }
        return roleList.stream()
                .map(role -> {
                    AdminRoleOptionVO item = new AdminRoleOptionVO();
                    item.setRoleCode(role.getRoleCode());
                    item.setRoleName(role.getRoleName());
                    return item;
                })
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserProfile(Long userId, AdminUserUpdateRequest request, String token) {
        LoginUserVO adminUser = requireAdmin(token);
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        SysUser targetUser = requireTargetUser(userId);
        if (isSuperAdminAccount(targetUser) && !Objects.equals(adminUser.getId(), targetUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅超管本人可编辑超管资料");
        }

        boolean changed = false;
        if (request.getRealName() != null) {
            String realName = request.getRealName().trim();
            if (!StringUtils.hasText(realName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "真实姓名不能为空");
            }
            targetUser.setRealName(realName);
            changed = true;
        }

        if (request.getStudentNo() != null) {
            String studentNo = normalizeNullableText(request.getStudentNo());
            if (studentNo != null) {
                ensureUniqueStudentNo(studentNo, userId);
            }
            targetUser.setStudentNo(studentNo);
            changed = true;
        }

        if (request.getEmail() != null) {
            String email = normalizeNullableText(request.getEmail());
            if (email != null) {
                if (!EMAIL_PATTERN.matcher(email).matches()) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
                }
                ensureUniqueEmail(email, userId);
            }
            targetUser.setEmail(email);
            changed = true;
        }

        if (request.getPhone() != null) {
            String phone = normalizeNullableText(request.getPhone());
            if (phone != null) {
                ensureUniquePhone(phone, userId);
            }
            targetUser.setPhone(phone);
            changed = true;
        }

        if (request.getGender() != null) {
            int gender = request.getGender();
            if (gender < 0 || gender > 2) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "性别参数错误");
            }
            targetUser.setGender(gender);
            changed = true;
        }

        if (!changed) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "没有可更新的字段");
        }
        return sysUserMapper.updateById(targetUser) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserStatus(Long userId, AdminUserStatusUpdateRequest request, String token) {
        LoginUserVO adminUser = requireAdmin(token);
        if (request == null || request.getStatus() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        int status = request.getStatus();
        if (status != 0 && status != 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态参数错误");
        }

        SysUser targetUser = requireTargetUser(userId);
        if (isSuperAdminAccount(targetUser)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "超管账号不允许执行启用/禁用操作");
        }
        if (Objects.equals(adminUser.getId(), targetUser.getId()) && status == 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "不能禁用当前登录管理员账号");
        }
        if (Objects.equals(targetUser.getStatus(), status)) {
            return true;
        }
        targetUser.setStatus(status);
        return sysUserMapper.updateById(targetUser) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserRoles(Long userId, AdminUserRolesUpdateRequest request, String token) {
        LoginUserVO adminUser = requireAdmin(token);
        if (request == null || CollectionUtils.isEmpty(request.getRoleCodes())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "角色列表不能为空");
        }
        SysUser targetUser = requireTargetUser(userId);
        if (isSuperAdminAccount(targetUser)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "超管账号不允许修改角色");
        }

        List<String> normalizedCodes = request.getRoleCodes().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(item -> item.toUpperCase())
                .distinct()
                .toList();
        if (normalizedCodes.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "角色列表不能为空");
        }
        if (normalizedCodes.size() != 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "角色仅支持单选");
        }
        if (!ALLOWED_ROLE_CODES.containsAll(normalizedCodes)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "存在非法角色");
        }
        if (Objects.equals(adminUser.getId(), userId) && !normalizedCodes.contains(ROLE_ADMIN)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "不能移除当前登录管理员的 ADMIN 角色");
        }

        List<SysRole> roles = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getRoleCode, normalizedCodes));
        if (CollectionUtils.isEmpty(roles)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "角色不存在");
        }
        Map<String, SysRole> roleMap = roles.stream()
                .collect(Collectors.toMap(SysRole::getRoleCode, item -> item, (a, b) -> a, LinkedHashMap::new));
        for (String code : normalizedCodes) {
            if (!roleMap.containsKey(code)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "角色不存在: " + code);
            }
        }

        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
        for (String code : normalizedCodes) {
            SysRole role = roleMap.get(code);
            SysUserRole relation = new SysUserRole();
            relation.setUserId(userId);
            relation.setRoleId(role.getId());
            sysUserRoleMapper.insert(relation);
        }
        return true;
    }

    private RoleBundle loadRoleBundle(Set<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return new RoleBundle(Map.of(), Map.of());
        }
        List<SysUserRole> relations = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .in(SysUserRole::getUserId, userIds));
        if (CollectionUtils.isEmpty(relations)) {
            return new RoleBundle(Map.of(), Map.of());
        }

        Map<Long, List<Long>> userRoleIdMap = new LinkedHashMap<>();
        Set<Long> roleIds = new LinkedHashSet<>();
        for (SysUserRole relation : relations) {
            if (relation.getUserId() == null || relation.getRoleId() == null) {
                continue;
            }
            userRoleIdMap.computeIfAbsent(relation.getUserId(), key -> new ArrayList<>()).add(relation.getRoleId());
            roleIds.add(relation.getRoleId());
        }
        if (roleIds.isEmpty()) {
            return new RoleBundle(Map.of(), Map.of());
        }

        Map<Long, SysRole> roleMap = sysRoleMapper.selectBatchIds(roleIds).stream()
                .collect(Collectors.toMap(SysRole::getId, item -> item, (a, b) -> a, LinkedHashMap::new));

        Map<Long, List<String>> codeMap = new LinkedHashMap<>();
        Map<Long, List<String>> nameMap = new LinkedHashMap<>();
        for (Map.Entry<Long, List<Long>> entry : userRoleIdMap.entrySet()) {
            List<SysRole> roleList = entry.getValue().stream()
                    .map(roleMap::get)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparingInt(item -> rolePriority(item.getRoleCode())))
                    .toList();
            codeMap.put(entry.getKey(), roleList.stream().map(SysRole::getRoleCode).toList());
            nameMap.put(entry.getKey(), roleList.stream().map(SysRole::getRoleName).toList());
        }
        return new RoleBundle(codeMap, nameMap);
    }

    private int rolePriority(String roleCode) {
        if (ROLE_ADMIN.equals(roleCode)) {
            return 1;
        }
        if (ROLE_TEACHER.equals(roleCode)) {
            return 2;
        }
        if (ROLE_STUDENT.equals(roleCode)) {
            return 3;
        }
        return 99;
    }

    private AdminUserVO toAdminUserVO(SysUser user, RoleBundle roleBundle) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setStudentNo(user.getStudentNo());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());
        vo.setStatus(user.getStatus());
        vo.setSuperAdmin(isSuperAdminAccount(user));
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreateTime(user.getCreateTime());
        vo.setRoleCodes(roleBundle.codeMap().getOrDefault(user.getId(), List.of()));
        vo.setRoleNames(roleBundle.nameMap().getOrDefault(user.getId(), List.of()));
        return vo;
    }

    private void ensureUniqueEmail(String email, Long userId) {
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmail, email)
                .ne(SysUser::getId, userId));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱已被占用");
        }
    }

    private void ensureUniquePhone(String phone, Long userId) {
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getPhone, phone)
                .ne(SysUser::getId, userId));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号已被占用");
        }
    }

    private void ensureUniqueStudentNo(String studentNo, Long userId) {
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStudentNo, studentNo)
                .ne(SysUser::getId, userId));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "学号/工号已被占用");
        }
    }

    private SysUser requireTargetUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不合法");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        return user;
    }

    private LoginUserVO requireAdmin(String token) {
        LoginUserVO loginUser = authService.getLoginUser(token);
        if (loginUser == null || CollectionUtils.isEmpty(loginUser.getRoles()) || !loginUser.getRoles().contains(ROLE_ADMIN)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅管理员可操作");
        }
        return loginUser;
    }

    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return keyword.trim();
    }

    private String normalizeRoleCodeNullable(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return null;
        }
        return roleCode.trim().toUpperCase();
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }

    private Page<AdminUserVO> emptyPage(long current, long pageSize) {
        return emptyPage(current, pageSize, 0);
    }

    private Page<AdminUserVO> emptyPage(long current, long pageSize, long total) {
        Page<AdminUserVO> page = new Page<>(current, pageSize, total);
        page.setRecords(List.of());
        return page;
    }

    private boolean isSuperAdminAccount(SysUser user) {
        if (user == null || !StringUtils.hasText(user.getUsername())) {
            return false;
        }
        return SUPER_ADMIN_USERNAME.equalsIgnoreCase(user.getUsername().trim());
    }

    private record RoleBundle(Map<Long, List<String>> codeMap, Map<Long, List<String>> nameMap) {
    }
}
