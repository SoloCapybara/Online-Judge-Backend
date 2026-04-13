package com.dxd.oj.model.dto.contest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "比赛报名请求")
public class ContestRegisterRequest {

    @Schema(description = "访问密码（PRIVATE 比赛需填写）", example = "123456")
    private String accessPassword;
}
