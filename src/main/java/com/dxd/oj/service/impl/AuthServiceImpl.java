package com.dxd.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dxd.oj.common.ErrorCode;
import com.dxd.oj.exception.BusinessException;
import com.dxd.oj.mapper.SysRoleMapper;
import com.dxd.oj.mapper.SysUserMapper;
import com.dxd.oj.mapper.SysUserRoleMapper;
import com.dxd.oj.model.dto.auth.UserRegisterRequest;
import com.dxd.oj.model.entity.SysRole;
import com.dxd.oj.model.entity.SysUser;
import com.dxd.oj.model.entity.SysUserRole;
import com.dxd.oj.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_ROLE_CODE = "STUDENT";

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(UserRegisterRequest request) {
        validateRegisterRequest(request);

        String username = request.getUsername().trim();

        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName().trim());
        user.setStatus(1);
        user.setGender(0);

        int inserted = sysUserMapper.insert(user);
        if (inserted <= 0 || user.getId() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户注册失败");
        }

        SysRole studentRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleCode, DEFAULT_ROLE_CODE)
                        .last("limit 1")
        );
        if (studentRole == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "默认学生角色不存在");
        }

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(studentRole.getId());

        int roleInserted = sysUserRoleMapper.insert(userRole);
        if (roleInserted <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户角色绑定失败");
        }

        return user.getId();
    }

    private void validateRegisterRequest(UserRegisterRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        if (!StringUtils.hasText(request.getUsername())
                || !StringUtils.hasText(request.getPassword())
                || !StringUtils.hasText(request.getCheckPassword())
                || !StringUtils.hasText(request.getRealName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        if (request.getUsername().trim().length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名长度不能小于4位");
        }

        if (request.getPassword().length() < 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不能小于6位");
        }

        if (!request.getPassword().equals(request.getCheckPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        if (request.getRealName().trim().length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "真实姓名长度不能超过50位");
        }
    }
}