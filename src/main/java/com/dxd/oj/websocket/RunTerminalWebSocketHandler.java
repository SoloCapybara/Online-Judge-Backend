package com.dxd.oj.websocket;

import com.dxd.oj.model.dto.run.RunCodeRequest;
import com.dxd.oj.model.vo.RunCodeResponse;
import com.dxd.oj.service.RunTerminalEventSink;
import com.dxd.oj.service.RunTerminalService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class RunTerminalWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final RunTerminalService runTerminalService;
    private final ConcurrentHashMap<String, SessionContext> sessionContexts = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = extractToken(session.getUri());
        if (!StringUtils.hasText(token)) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("缺少登录凭证"));
            return;
        }
        sessionContexts.put(session.getId(), new SessionContext(token));
        sendJson(session, Map.of("type", "status", "statusText", "终端已连接"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        SessionContext context = sessionContexts.get(session.getId());
        if (context == null) {
            sendJson(session, Map.of("type", "error", "message", "会话不存在"));
            return;
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(message.getPayload());
        } catch (JsonProcessingException e) {
            sendJson(session, Map.of("type", "error", "message", "消息格式错误"));
            return;
        }

        String type = root.path("type").asText("");
        switch (type) {
            case "start" -> handleStart(session, context, root.path("payload"));
            case "stdin" -> handleStdin(session, root.path("data").asText(""));
            case "eof" -> runTerminalService.closeInput(session.getId());
            case "stop" -> runTerminalService.stop(session.getId());
            case "ping" -> sendJson(session, Map.of("type", "pong"));
            default -> sendJson(session, Map.of("type", "error", "message", "未知消息类型"));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        runTerminalService.stop(session.getId());
        sessionContexts.remove(session.getId());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        runTerminalService.stop(session.getId());
        sessionContexts.remove(session.getId());
    }

    private void handleStart(WebSocketSession session, SessionContext context, JsonNode payloadNode) {
        if (payloadNode == null || payloadNode.isMissingNode() || payloadNode.isNull()) {
            sendJsonQuietly(session, Map.of("type", "error", "message", "缺少运行参数"));
            return;
        }

        RunCodeRequest request;
        try {
            request = objectMapper.treeToValue(payloadNode, RunCodeRequest.class);
        } catch (JsonProcessingException e) {
            sendJsonQuietly(session, Map.of("type", "error", "message", "运行参数格式错误"));
            return;
        }

        runTerminalService.start(session.getId(), context.token, request, new RunTerminalEventSink() {
            @Override
            public void onStatus(String statusText) {
                sendJsonQuietly(session, Map.of("type", "status", "statusText", statusText));
            }

            @Override
            public void onStdout(String chunk) {
                sendJsonQuietly(session, Map.of("type", "stdout", "chunk", chunk));
            }

            @Override
            public void onStderr(String chunk) {
                sendJsonQuietly(session, Map.of("type", "stderr", "chunk", chunk));
            }

            @Override
            public void onResult(RunCodeResponse result) {
                sendJsonQuietly(session, Map.of("type", "result", "data", result));
            }

            @Override
            public void onError(String message) {
                sendJsonQuietly(session, Map.of("type", "error", "message", message));
            }
        });
    }

    private void handleStdin(WebSocketSession session, String data) {
        if (!StringUtils.hasText(data)) {
            return;
        }
        runTerminalService.sendInput(session.getId(), data);
    }

    private void sendJson(WebSocketSession session, Object payload) throws IOException {
        if (!session.isOpen()) {
            return;
        }
        String text = objectMapper.writeValueAsString(payload);
        SessionContext context = sessionContexts.get(session.getId());
        Object lock = context == null ? session : context.sendLock;
        synchronized (lock) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(text));
            }
        }
    }

    private void sendJsonQuietly(WebSocketSession session, Object payload) {
        try {
            sendJson(session, payload);
        } catch (Exception e) {
            log.debug("run terminal send message failed: {}", e.getMessage());
        }
    }

    private String extractToken(URI uri) {
        if (uri == null) {
            return null;
        }
        return UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("token");
    }

    private static class SessionContext {
        private final String token;
        private final Object sendLock = new Object();

        private SessionContext(String token) {
            this.token = token;
        }
    }
}
