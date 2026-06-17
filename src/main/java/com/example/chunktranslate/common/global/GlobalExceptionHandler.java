package com.example.chunktranslate.common.global;

import com.example.chunktranslate.common.exception.BusinessException;
import com.example.chunktranslate.common.result.Result;
import com.example.chunktranslate.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@Slf4j
@RestControllerAdvice(basePackages = "com.example.chunktranslate.controller")
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<?> handleValidationException(Exception e) {
        log.warn("参数校验失败: {}", e.getMessage());
        return Result.fail(ResultCode.BAD_REQUEST);
    }

    /**
     * 文件上传大小超限
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("文件上传超限: {}", e.getMessage());
        return Result.fail(ResultCode.FILE_SIZE_EXCEED);
    }

    /**
     * 文件上传参数缺失（未选择文件或请求格式不是 multipart）
     */
    @ExceptionHandler({MultipartException.class, MissingServletRequestPartException.class})
    public Result<?> handleMultipartException(Exception e) {
        log.warn("文件上传参数错误: {}", e.getMessage());
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), "请上传文件，且请求格式必须为 multipart/form-data");
    }

    /**
     * 兜底：未知异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ResultCode.INTERNAL_ERROR);
    }
}
