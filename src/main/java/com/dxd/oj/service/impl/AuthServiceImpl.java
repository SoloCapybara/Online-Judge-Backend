package com.dxd.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dxd.oj.common.ErrorCode;
import com.dxd.oj.common.JwtUtils;
import com.dxd.oj.exception.BusinessException;
import com.dxd.oj.mapper.LoginLogMapper;
import com.dxd.oj.mapper.SysRoleMapper;
import com.dxd.oj.mapper.SysUserMapper;
import com.dxd.oj.mapper.SysUserRoleMapper;
import com.dxd.oj.model.dto.auth.UserLoginRequest;
import com.dxd.oj.model.dto.auth.UserRegisterRequest;
import com.dxd.oj.model.entity.LoginLog;
import com.dxd.oj.model.entity.SysRole;
import com.dxd.oj.model.entity.SysUser;
import com.dxd.oj.model.entity.SysUserRole;
import com.dxd.oj.model.vo.LoginUserVO;
import com.dxd.oj.model.vo.LoginVO;
import com.dxd.oj.service.AuthService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String SUPER_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ROLE_CODE = "STUDENT";
    private static final String LOGIN_TYPE_PASSWORD = "PASSWORD";

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final LoginLogMapper loginLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${oj.auth.redis-token-prefix:oj:auth:token:}")
    private String redisTokenPrefix;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(UserRegisterRequest request) {
        validateRegisterRequest(request);

        String username = request.getUsername().trim();
        String email = StringUtils.hasText(request.getEmail()) ? request.getEmail().trim() : null;
        log.info("认证服务-注册开始：username={}, 是否填写邮箱={}", username, StringUtils.hasText(email));

        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名已存在");
        }
        if (StringUtils.hasText(email)) {
            Long emailCount = sysUserMapper.selectCount(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getEmail, email)
            );
            if (emailCount != null && emailCount > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱已被注册");
            }
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName().trim());
        user.setEmail(email);
        user.setStatus(1);
        user.setGender(0);

        int inserted = sysUserMapper.insert(user);
        if (inserted <= 0 || user.getId() == null) {
            log.error("认证服务-注册失败：用户插入失败，username={}", username);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户注册失败");
        }

        SysRole studentRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleCode, DEFAULT_ROLE_CODE)
                        .last("limit 1")
        );
        if (studentRole == null) {
            log.error("认证服务-注册失败：默认角色不存在，roleCode={}", DEFAULT_ROLE_CODE);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "默认学生角色不存在");
        }

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(studentRole.getId());

        int roleInserted = sysUserRoleMapper.insert(userRole);
        if (roleInserted <= 0) {
            log.error("认证服务-注册失败：用户角色绑定失败，userId={}, roleId={}", user.getId(), studentRole.getId());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户角色绑定失败");
        }

        log.info("认证服务-注册成功：userId={}, username={}", user.getId(), username);
        return user.getId();
    }

    @Override
    public LoginVO login(UserLoginRequest request, String ip, String userAgent) {
        if (request == null || !StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码不能为空");
        }

        String account = request.getUsername().trim();
        log.info("认证服务-登录开始：account={}, ip={}", account, ip);
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .and(wrapper -> wrapper
                        .eq(SysUser::getUsername, account)
                        .or()
                        .eq(SysUser::getEmail, account))
                .last("limit 1"));
        if (user == null) {
            saveLoginLog(null, account, 0, ip, userAgent, "用户不存在");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");
        }
        if (!Objects.equals(user.getStatus(), 1)) {
            saveLoginLog(user.getId(), account, 0, ip, userAgent, "账号已禁用");
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "账号已禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            saveLoginLog(user.getId(), account, 0, ip, userAgent, "密码错误");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        String redisKey = buildTokenKey(token);
        stringRedisTemplate.opsForValue().set(redisKey, String.valueOf(user.getId()), jwtUtils.getExpireSeconds(), TimeUnit.SECONDS);

        user.setLastLoginTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
        saveLoginLog(user.getId(), user.getUsername(), 1, ip, userAgent, null);

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUserInfo(buildLoginUserVO(user));
        log.info("认证服务-登录成功：userId={}, username={}, ip={}", user.getId(), user.getUsername(), ip);
        return loginVO;
    }

    @Override
    public LoginUserVO getLoginUser(String token) {
        Claims claims = parseAndCheckToken(token);
        Long userId = getUserIdFromClaims(claims);
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户不存在");
        }
        return buildLoginUserVO(user);
    }

    @Override
    public boolean logout(String token) {
        if (!StringUtils.hasText(token)) {
            return true;
        }
        stringRedisTemplate.delete(buildTokenKey(token));
        log.info("认证服务-退出成功");
        return true;
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

        String username = request.getUsername().trim();
        if (SUPER_ADMIN_USERNAME.equalsIgnoreCase(username)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名已被系统保留");
        }

        if (username.length() < 4) {
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

    private Claims parseAndCheckToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        try {
            Claims claims = jwtUtils.parseToken(token);
            String redisValue = stringRedisTemplate.opsForValue().get(buildTokenKey(token));
            if (!StringUtils.hasText(redisValue)) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "登录状态已失效");
            }
            return claims;
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "登录状态无效");
        }
    }

    private Long getUserIdFromClaims(Claims claims) {
        Object userIdObj = claims.get("userId");
        if (!(userIdObj instanceof Number number)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "登录状态无效");
        }
        return number.longValue();
    }

    private String buildTokenKey(String token) {
        return redisTokenPrefix + token;
    }

    private LoginUserVO buildLoginUserVO(SysUser user) {
        LoginUserVO loginUserVO = new LoginUserVO();
        loginUserVO.setId(user.getId());
        loginUserVO.setUsername(user.getUsername());
        loginUserVO.setRealName(user.getRealName());
        loginUserVO.setAvatar(user.getAvatar());
        loginUserVO.setStatus(user.getStatus());
        loginUserVO.setRoles(getRoleCodesByUserId(user.getId()));
        return loginUserVO;
    }

    private List<String> getRoleCodesByUserId(Long userId) {
        List<SysUserRole> userRoleList = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
        if (CollectionUtils.isEmpty(userRoleList)) {
            return Collections.emptyList();
        }
        List<Long> roleIds = userRoleList.stream().map(SysUserRole::getRoleId).toList();
        List<SysRole> roleList = sysRoleMapper.selectBatchIds(roleIds);
        if (CollectionUtils.isEmpty(roleList)) {
            return Collections.emptyList();
        }
        return roleList.stream().map(SysRole::getRoleCode).collect(Collectors.toList());
    }

    private void saveLoginLog(Long userId, String username, Integer loginStatus, String ip, String userAgent, String failReason) {
        LoginLog loginLog = new LoginLog();
        loginLog.setUserId(userId);
        loginLog.setUsername(username);
        loginLog.setLoginType(LOGIN_TYPE_PASSWORD);
        loginLog.setLoginStatus(loginStatus);
        loginLog.setIp(ip);
        loginLog.setUserAgent(userAgent);
        loginLog.setFailReason(failReason);
        loginLogMapper.insert(loginLog);
    }
}
