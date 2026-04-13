package com.dxd.oj.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dxd.oj.mapper.SysRoleMapper;
import com.dxd.oj.mapper.SysUserMapper;
import com.dxd.oj.mapper.SysUserRoleMapper;
import com.dxd.oj.model.entity.SysRole;
import com.dxd.oj.model.entity.SysUser;
import com.dxd.oj.model.entity.SysUserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Ensure a built-in super admin account exists on startup.
 * The logic is idempotent: no duplicate users/roles/relations are inserted.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminBootstrapRunner implements ApplicationRunner {

    private static final String SUPER_ADMIN_USERNAME = "admin";
    private static final String ADMIN_ROLE_CODE = "ADMIN";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin@123456";

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${oj.bootstrap.super-admin.enabled:true}")
    private boolean enabled;

    @Value("${oj.bootstrap.super-admin.password:" + DEFAULT_ADMIN_PASSWORD + "}")
    private String password;

    @Value("${oj.bootstrap.super-admin.real-name:System Admin}")
    private String realName;

    @Value("${oj.bootstrap.super-admin.force-reset-password:false}")
    private boolean forceResetPassword;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("super admin bootstrap disabled");
            return;
        }

        if (!StringUtils.hasText(password)) {
            log.warn("skip super admin bootstrap: password is blank");
            return;
        }

        SysRole adminRole = ensureAdminRole();
        SysUser adminUser = ensureAdminUser();
        ensureUserRoleRelation(adminUser.getId(), adminRole.getId());

        if (DEFAULT_ADMIN_PASSWORD.equals(password)) {
            log.warn("super admin '{}' is using default password, change it in production", SUPER_ADMIN_USERNAME);
        }

        log.info("super admin bootstrap finished: username={}", SUPER_ADMIN_USERNAME);
    }

    private SysRole ensureAdminRole() {
        SysRole role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, ADMIN_ROLE_CODE)
                .last("limit 1"));
        if (role != null) {
            return role;
        }

        SysRole newRole = new SysRole();
        newRole.setRoleCode(ADMIN_ROLE_CODE);
        newRole.setRoleName("管理员");
        newRole.setDescription("系统管理员角色");
        sysRoleMapper.insert(newRole);
        return newRole;
    }

    private SysUser ensureAdminUser() {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, SUPER_ADMIN_USERNAME)
                .last("limit 1"));
        if (user == null) {
            SysUser newUser = new SysUser();
            newUser.setUsername(SUPER_ADMIN_USERNAME);
            newUser.setPassword(passwordEncoder.encode(password));
            newUser.setRealName(StringUtils.hasText(realName) ? realName.trim() : "System Admin");
            newUser.setStatus(1);
            newUser.setGender(0);
            sysUserMapper.insert(newUser);
            return newUser;
        }

        boolean updated = false;
        if (user.getStatus() == null || user.getStatus() != 1) {
            user.setStatus(1);
            updated = true;
        }
        if (forceResetPassword) {
            user.setPassword(passwordEncoder.encode(password));
            updated = true;
        }
        if (updated) {
            sysUserMapper.updateById(user);
        }
        return user;
    }

    private void ensureUserRoleRelation(Long userId, Long roleId) {
        Long count = sysUserRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getRoleId, roleId));
        if (count != null && count > 0) {
            return;
        }

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        sysUserRoleMapper.insert(userRole);
    }
}
