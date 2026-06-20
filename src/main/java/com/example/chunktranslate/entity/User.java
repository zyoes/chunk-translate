package com.example.chunktranslate.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.chunktranslate.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户实体，对应 sys_user 表。
 * <p>支持本地注册和 GitHub OAuth2 登录两种方式，通过 provider 字段区分。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends BaseEntity {

    /** 用户名（登录用） */
    private String username;

    /** 邮箱 */
    private String email;

    /** BCrypt 加密后的密码（OAuth2 用户可为空） */
    private String password;

    /** 登录提供商：local 或 github，对应 {@link com.example.chunktranslate.common.enums.ProviderType} */
    private String provider;

    /** 第三方平台的用户唯一标识（OAuth2 用户才有值） */
    private String providerId;

    /** 头像 URL */
    private String avatarUrl;

    /** 用户角色，对应 {@link com.example.chunktranslate.common.enums.UserRole} */
    private String role;

    /** 账户状态：0=正常，1=禁用 */
    private Integer status;

    /** 最后登录时间 */
    private LocalDateTime lastLoginAt;
}
