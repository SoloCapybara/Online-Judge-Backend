package com.dxd.oj.common;

public final class ResultUtils {

    private ResultUtils() {
    }

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(ErrorCode.SUCCESS.getCode(), data, ErrorCode.SUCCESS.getMessage());
    }

    public static BaseResponse<Boolean> success() {
        return success(Boolean.TRUE);
    }

    public static BaseResponse<Object> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode.getCode(), null, errorCode.getMessage());
    }

    public static BaseResponse<Object> error(int code, String message) {
        return new BaseResponse<>(code, null, message);
    }
}
