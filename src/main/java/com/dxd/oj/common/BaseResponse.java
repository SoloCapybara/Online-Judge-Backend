package com.dxd.oj.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统一响应体")
public class BaseResponse<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "业务状态码：0=成功，40000=参数错误，40100=未登录，40101=无权限，40400=数据不存在，50000=系统异常，50001=操作失败", example = "0")
    private int code;

    @Schema(description = "响应数据")
    private T data;

    @Schema(description = "响应消息", example = "ok")
    private String message;
}
