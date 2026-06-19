package com.example.chunktranslate.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 业务错误码 1xxx
    FILE_UPLOAD_FAIL(1001, "文件上传失败"),
    FILE_TYPE_NOT_SUPPORT(1002, "不支持的文件类型"),
    FILE_SIZE_EXCEED(1003, "文件大小超过限制"),
    DOCUMENT_NOT_FOUND(1004, "文档不存在"),
    DOCUMENT_PARSE_FAIL(1005, "文档解析失败"),

    CHUNK_NOT_FOUND(1010, "分块不存在"),

    TRANSLATION_FAIL(1020, "翻译失败"),
    TRANSLATION_IN_PROGRESS(1021, "翻译进行中"),

    EXPORT_FAIL(1030, "导出失败"),

    // 认证相关错误码 2xxx
    USER_NOT_FOUND(2001, "用户不存在"),
    USER_DISABLED(2002, "用户已被禁用"),
    USERNAME_ALREADY_EXISTS(2003, "用户名已存在"),
    EMAIL_ALREADY_EXISTS(2004, "邮箱已被注册"),
    INVALID_CREDENTIALS(2005, "用户名或密码错误"),
    INVALID_TOKEN(2006, "无效的令牌"),
    TOKEN_EXPIRED(2007, "令牌已过期"),
    REFRESH_TOKEN_REVOKED(2008, "刷新令牌已被撤销"),
    REFRESH_TOKEN_EXPIRED(2009, "刷新令牌已过期");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
