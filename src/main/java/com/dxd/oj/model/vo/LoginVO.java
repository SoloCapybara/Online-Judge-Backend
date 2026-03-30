package com.dxd.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "登录响应数据")
public class LoginVO implements Serializable {

    @Schema(description = "JWT令牌，请在后续请求头中携带 Authorization: Bearer {token}")
    private String token;

    @Schema(description = "当前登录用户信息")
    private LoginUserVO userInfo;
}
