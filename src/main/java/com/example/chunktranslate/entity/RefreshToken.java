package com.example.chunktranslate.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.chunktranslate.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 刷新令牌实体，对应 refresh_token 表。
 * <p>JWT access token 过期后，客户端使用 refresh token 换取新的 access token。
 * 每个 refresh token 有独立的过期时间和吊销标记。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("refresh_token")
public class RefreshToken extends BaseEntity {

    /** 所属用户 ID */
    private Long userId;

    /** 随机生成的刷新令牌字符串 */
    private String token;

    /** 令牌过期时间 */
    private LocalDateTime expiresAt;

    /** 是否已吊销：0=有效，1=已吊销 */
    private Integer revoked;
}

