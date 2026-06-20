package com.example.chunktranslate.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新个人资料请求 DTO，支持修改用户名和头像。
 */
@Data
@Schema(description = "更新个人资料请求")
public class UpdateProfileRequest {

    @Size(min = 3, max = 50, message = "用户名长度3-50")
    @Schema(description = "新用户名")
    private String username;

    @Schema(description = "头像URL")
    private String avatarUrl;
}