package com.example.chunktranslate.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重置密码请求 DTO，通过邮箱验证码重置密码（无需旧密码）。
 */
@Data
@Schema(description = "重置密码请求")
public class ResetPasswordRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "注册邮箱")
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "邮箱验证码")
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度6-100")
    @Schema(description = "新密码")
    private String newPassword;
}
