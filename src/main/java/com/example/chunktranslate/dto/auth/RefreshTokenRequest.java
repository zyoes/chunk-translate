package com.example.chunktranslate.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 令牌刷新请求 DTO，使用 refresh token 换取新的 access token。
 */
@Data
@Schema(description = "刷新令牌请求")
public class RefreshTokenRequest {

    @NotBlank(message = "刷新令牌不能为空")
    @Schema(description = "刷新令牌")
    private String refreshToken;
}