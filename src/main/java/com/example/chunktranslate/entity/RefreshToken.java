package com.example.chunktranslate.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.chunktranslate.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("refresh_token")
public class RefreshToken extends BaseEntity {

    private Long userId;

    private String token;

    private LocalDateTime expiresAt;

    private Integer revoked;
}

