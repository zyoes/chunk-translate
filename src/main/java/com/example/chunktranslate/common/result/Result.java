package com.example.chunktranslate.common.result;

import lombok.Data;

/**
 * 统一 API 响应体。
 * <p>所有 Controller 接口的返回值均包装为此类型，前端 Axios 拦截器通过 code 判断成功/失败。
 * 泛型 T 为具体的数据类型，当没有数据时调用无参的 {@link #success()} 方法。</p>
 *
 * @param <T> 响应数据的类型
 */
@Data
public class Result<T> {
    /** 状态码，与 HTTP 状态码对齐（200=成功，4xx/5xx=失败，1xxx/2xxx=业务错误） */
    private int code;
    /** 提示信息 */
    private String message;
    /** 响应数据 */
    private T data;

    private Result() {}

    /**
     * 返回带数据的成功响应。
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return code=200 的成功 Result
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(ResultCode.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    /**
     * 返回无数据的成功响应。
     *
     * @param <T> 数据类型
     * @return code=200、data=null 的 Result
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 通过预定义错误码返回失败响应。
     *
     * @param resultCode 错误码枚举
     * @param <T>        数据类型
     * @return 包含错误信息的 Result
     */
    public static <T> Result<T> fail(ResultCode resultCode) {
        Result<T> result = new Result<>();
        result.setCode(resultCode.getCode());
        result.setMessage(resultCode.getMessage());
        return result;
    }

    /**
     * 通过自定义错误码和消息返回失败响应。
     *
     * @param code    自定义错误码
     * @param message 错误描述
     * @param <T>     数据类型
     * @return 包含自定义错误信息的 Result
     */
    public static <T> Result<T> fail(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
