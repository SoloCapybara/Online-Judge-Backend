package com.dxd.oj.service;

import com.dxd.oj.model.vo.RunCodeResponse;

public interface RunTerminalEventSink {

    void onStatus(String statusText);

    void onStdout(String chunk);

    void onStderr(String chunk);

    void onResult(RunCodeResponse result);

    void onError(String message);
}

