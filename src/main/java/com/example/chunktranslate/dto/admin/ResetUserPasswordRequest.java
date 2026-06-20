package com.example.chunktranslate.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员重置用户密码请求。
 */
@Data
public class ResetUserPasswordRequest {
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度6-100")
    private String password;
}
