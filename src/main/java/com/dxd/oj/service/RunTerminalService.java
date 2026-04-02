package com.dxd.oj.service;

import com.dxd.oj.model.dto.run.RunCodeRequest;

public interface RunTerminalService {

    void start(String sessionId, String token, RunCodeRequest request, RunTerminalEventSink eventSink);

    void sendInput(String sessionId, String data);

    void closeInput(String sessionId);

    void stop(String sessionId);
}
