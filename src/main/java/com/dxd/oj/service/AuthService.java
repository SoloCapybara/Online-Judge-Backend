package com.dxd.oj.service;

import com.dxd.oj.model.dto.auth.UserRegisterRequest;

public interface AuthService {

    Long register(UserRegisterRequest request);
}
