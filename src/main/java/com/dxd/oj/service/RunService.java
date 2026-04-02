package com.dxd.oj.service;

import com.dxd.oj.model.dto.run.RunCodeRequest;
import com.dxd.oj.model.vo.RunCodeResponse;

public interface RunService {

    RunCodeResponse runCode(RunCodeRequest request, String token);

    RunCodeResponse runCodeStream(RunCodeRequest request, String token, RunStreamCallback callback);
}
