package com.dxd.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "注册结果")
public class RegisterVO implements Serializable {

    @Schema(description = "新注册用户ID", example = "2038605982539427842")
    private Long userId;
}
