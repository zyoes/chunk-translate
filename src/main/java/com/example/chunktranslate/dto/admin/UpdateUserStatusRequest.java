package com.example.chunktranslate.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员启用/禁用用户请求。
 */
@Data
public class UpdateUserStatusRequest {
    @NotNull(message = "状态不能为空")
    private Integer status;
}
