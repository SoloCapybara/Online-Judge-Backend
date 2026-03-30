package com.dxd.oj.controller;

import com.dxd.oj.common.BaseResponse;
import com.dxd.oj.common.ResultUtils;
import com.dxd.oj.model.dto.user.UserPasswordUpdateRequest;
import com.dxd.oj.model.dto.user.UserProfileUpdateRequest;
import com.dxd.oj.model.vo.UserProfileVO;
import com.dxd.oj.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "用户中心")
@RestController
@RequestMapping("/user/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @Operation(summary = "获取当前用户个人信息")
    @GetMapping
    public BaseResponse<UserProfileVO> getCurrentUserProfile(HttpServletRequest request) {
        String token = extractToken(request);
        UserProfileVO profileVO = userProfileService.getCurrentUserProfile(token);
        return ResultUtils.success(profileVO);
    }

    @Operation(summary = "更新当前用户个人信息")
    @PutMapping
    public BaseResponse<UserProfileVO> updateCurrentUserProfile(@Valid @RequestBody UserProfileUpdateRequest updateRequest,
                                                                HttpServletRequest request) {
        String token = extractToken(request);
        UserProfileVO profileVO = userProfileService.updateCurrentUserProfile(token, updateRequest);
        return ResultUtils.success(profileVO);
    }

    @Operation(summary = "上传头像")
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<UserProfileVO> uploadAvatar(@RequestParam("file") MultipartFile file,
                                                    HttpServletRequest request) {
        String token = extractToken(request);
        UserProfileVO profileVO = userProfileService.uploadAvatar(token, file);
        return ResultUtils.success(profileVO);
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public BaseResponse<Boolean> updatePassword(@Valid @RequestBody UserPasswordUpdateRequest updateRequest,
                                                HttpServletRequest request) {
        String token = extractToken(request);
        boolean result = userProfileService.updatePassword(token, updateRequest);
        return ResultUtils.success(result);
    }

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
