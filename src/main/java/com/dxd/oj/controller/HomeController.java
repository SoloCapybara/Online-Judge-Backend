package com.dxd.oj.controller;

import com.dxd.oj.common.BaseResponse;
import com.dxd.oj.common.ResultUtils;
import com.dxd.oj.model.vo.HomeOverviewVO;
import com.dxd.oj.service.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "首页")
@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @Operation(summary = "首页概览", description = "返回首页统计卡片、最近比赛赛程与最近提交数据。")
    @GetMapping("/overview")
    public BaseResponse<HomeOverviewVO> getHomeOverview(HttpServletRequest request) {
        String token = extractToken(request);
        HomeOverviewVO data = homeService.getHomeOverview(token);
        return ResultUtils.success(data);
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
