package com.dxd.oj.config;

import com.dxd.oj.websocket.RunTerminalWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class RunWebSocketConfig implements WebSocketConfigurer {

    private final RunTerminalWebSocketHandler runTerminalWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(runTerminalWebSocketHandler, "/run/ws")
                .setAllowedOriginPatterns("*");
    }
}

