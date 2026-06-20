package com.example.chunktranslate.common.exception;

import com.example.chunktranslate.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常，用于在服务层抛出可预期的业务错误。
 * <p>配合 {@link com.example.chunktranslate.common.global.GlobalExceptionHandler GlobalExceptionHandler}
 * 统一转换为 {@link com.example.chunktranslate.common.result.Result Result} 响应返回给前端。</p>
 */
@Getter
public class BusinessException extends RuntimeException {
    /** 业务错误码，对应 {@link com.example.chunktranslate.common.result.ResultCode} */
    private final int code;

    /**
     * 通过预定义的 {@link com.example.chunktranslate.common.result.ResultCode ResultCode} 创建异常。
     *
     * @param resultCode 包含错误码和错误信息的枚举
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    /**
     * 通过自定义错误码和消息创建异常。
     *
     * @param code    自定义业务错误码
     * @param message 错误描述
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
