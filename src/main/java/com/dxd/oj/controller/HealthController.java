package com.dxd.oj.controller;

import com.dxd.oj.common.BaseResponse;
import com.dxd.oj.common.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "系统接口")
@RestController
public class HealthController {

    @Operation(summary = "健康检查", description = "用于探活和联通性检查，返回 status=ok 表示服务可用。")
    @GetMapping("/health")
    public BaseResponse<Map<String, String>> health() {
        return ResultUtils.success(Map.of("status", "ok"));
    }
}
