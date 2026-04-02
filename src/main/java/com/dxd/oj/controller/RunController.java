package com.dxd.oj.controller;

import com.dxd.oj.common.BaseResponse;
import com.dxd.oj.common.ResultUtils;
import com.dxd.oj.model.dto.run.RunCodeRequest;
import com.dxd.oj.model.vo.RunCodeResponse;
import com.dxd.oj.service.RunService;
import com.dxd.oj.service.RunStreamCallback;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Tag(name = "代码运行")
@RestController
@RequestMapping("/run")
@RequiredArgsConstructor
@Slf4j
public class RunController {

    private final RunService runService;

    @Operation(summary = "自测运行代码", description = "用于前端“自测运行”按钮。本版本为本机编译执行，不计入提交记录。")
    @PostMapping
    public BaseResponse<RunCodeResponse> runCode(@Valid @RequestBody RunCodeRequest request, HttpServletRequest httpServletRequest) {
        String token = extractToken(httpServletRequest);
        RunCodeResponse response = runService.runCode(request, token);
        return ResultUtils.success(response);
    }

    @Operation(summary = "流式自测运行代码", description = "用于终端实时输出。返回 SSE 事件流。")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter runCodeStream(@Valid @RequestBody RunCodeRequest request, HttpServletRequest httpServletRequest) {
        String token = extractToken(httpServletRequest);
        SseEmitter emitter = new SseEmitter(120_000L);
        Object sendLock = new Object();

        emitter.onTimeout(emitter::complete);
        emitter.onError(ex -> log.warn("run stream emitter error", ex));

        CompletableFuture.runAsync(() -> {
            try {
                sendEvent(emitter, sendLock, "status", Map.of(
                        "type", "status",
                        "statusText", "已接收运行请求"
                ));

                RunCodeResponse result = runService.runCodeStream(request, token, new RunStreamCallback() {
                    @Override
                    public void onStatus(String statusText) {
                        if (!StringUtils.hasText(statusText)) {
                            return;
                        }
                        sendEventQuietly(emitter, sendLock, "status", Map.of(
                                "type", "status",
                                "statusText", statusText
                        ));
                    }

                    @Override
                    public void onStdout(String chunk) {
                        if (chunk == null || chunk.isEmpty()) {
                            return;
                        }
                        sendEventQuietly(emitter, sendLock, "stdout", Map.of(
                                "type", "stdout",
                                "stdoutChunk", chunk
                        ));
                    }

                    @Override
                    public void onStderr(String chunk) {
                        if (chunk == null || chunk.isEmpty()) {
                            return;
                        }
                        sendEventQuietly(emitter, sendLock, "stderr", Map.of(
                                "type", "stderr",
                                "stderrChunk", chunk
                        ));
                    }
                });

                Map<String, Object> resultPayload = new HashMap<>();
                resultPayload.put("type", "result");
                resultPayload.put("data", result);
                sendEvent(emitter, sendLock, "result", resultPayload);
                emitter.complete();
            } catch (Exception e) {
                log.error("run stream failed", e);
                sendEventQuietly(emitter, sendLock, "status", Map.of(
                        "type", "status",
                        "statusText", StringUtils.hasText(e.getMessage()) ? e.getMessage() : "流式运行失败",
                        "error", StringUtils.hasText(e.getMessage()) ? e.getMessage() : "流式运行失败"
                ));
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private void sendEvent(SseEmitter emitter, Object sendLock, String eventName, Object payload) throws IOException {
        synchronized (sendLock) {
            emitter.send(SseEmitter.event().name(eventName).data(payload, MediaType.APPLICATION_JSON));
        }
    }

    private void sendEventQuietly(SseEmitter emitter, Object sendLock, String eventName, Object payload) {
        try {
            sendEvent(emitter, sendLock, eventName, payload);
        } catch (IOException ignored) {
            // client disconnected
        }
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
