package com.example.chunktranslate.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 认证响应 DTO，包含 JWT access token、refresh token 和用户信息。
 */
@Data
@Schema(description = "认证响应")
public class AuthResponse {

    @Schema(description = "访问令牌")
    private String accessToken;

    @Schema(description = "刷新令牌")
    private String refreshToken;

    @Schema(description = "访问令牌过期时间（秒）")
    private long expiresIn;

    @Schema(description = "令牌类型")
    private String tokenType = "Bearer";

    @Schema(description = "用户信息")
    private UserInfoResponse user;
}