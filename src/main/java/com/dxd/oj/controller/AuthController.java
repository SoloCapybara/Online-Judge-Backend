package com.dxd.oj.controller;

import com.dxd.oj.common.BaseResponse;
import com.dxd.oj.common.ResultUtils;
import com.dxd.oj.model.dto.auth.UserLoginRequest;
import com.dxd.oj.model.dto.auth.UserRegisterRequest;
import com.dxd.oj.model.vo.LoginUserVO;
import com.dxd.oj.model.vo.LoginVO;
import com.dxd.oj.model.vo.RegisterVO;
import com.dxd.oj.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证模块")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户注册", description = "支持用户名注册，邮箱可选且唯一。注册成功后默认分配 STUDENT 角色。")
    @PostMapping("/register")
    public BaseResponse<RegisterVO> register(@Valid @RequestBody UserRegisterRequest request) {
        Long userId = authService.register(request);
        RegisterVO response = new RegisterVO();
        response.setUserId(userId);
        return ResultUtils.success(response);
    }

    @Operation(summary = "用户登录", description = "支持用户名或邮箱登录，登录成功返回JWT令牌。")
    @PostMapping("/login")
    public BaseResponse<LoginVO> login(@Valid @RequestBody UserLoginRequest request, HttpServletRequest httpServletRequest) {
        LoginVO loginVO = authService.login(request, getClientIp(httpServletRequest), httpServletRequest.getHeader("User-Agent"));
        return ResultUtils.success(loginVO);
    }

    @Operation(summary = "获取当前登录用户", description = "从请求头 Authorization 中解析 Bearer Token，返回当前登录用户信息。")
    @GetMapping("/me")
    public BaseResponse<LoginUserVO> me(HttpServletRequest httpServletRequest) {
        String token = extractToken(httpServletRequest);
        LoginUserVO loginUserVO = authService.getLoginUser(token);
        return ResultUtils.success(loginUserVO);
    }

    @Operation(summary = "退出登录", description = "使当前Token失效（删除Redis中的登录态）。")
    @PostMapping("/logout")
    public BaseResponse<Boolean> logout(HttpServletRequest httpServletRequest) {
        String token = extractToken(httpServletRequest);
        boolean result = authService.logout(token);
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

    @Parameter(hidden = true)
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            int commaIndex = xForwardedFor.indexOf(',');
            return commaIndex > 0 ? xForwardedFor.substring(0, commaIndex).trim() : xForwardedFor.trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }
}
