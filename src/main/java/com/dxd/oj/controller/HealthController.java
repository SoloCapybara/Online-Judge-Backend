package com.dxd.oj.controller;

import com.dxd.oj.common.BaseResponse;
import com.dxd.oj.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public BaseResponse<Map<String, String>> health() {
        return ResultUtils.success(Map.of("status", "ok"));
    }
}
