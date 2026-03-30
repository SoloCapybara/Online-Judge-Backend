package com.dxd.oj.service;

import com.dxd.oj.model.dto.auth.UserRegisterRequest;
import com.dxd.oj.model.dto.auth.UserLoginRequest;
import com.dxd.oj.model.vo.LoginUserVO;
import com.dxd.oj.model.vo.LoginVO;

public interface AuthService {

    Long register(UserRegisterRequest request);

    LoginVO login(UserLoginRequest request, String ip, String userAgent);

    LoginUserVO getLoginUser(String token);

    boolean logout(String token);
}
