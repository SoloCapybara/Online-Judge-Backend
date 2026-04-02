package com.dxd.oj.service;

public interface RunStreamCallback {

    void onStatus(String statusText);

    void onStdout(String chunk);

    void onStderr(String chunk);
}

