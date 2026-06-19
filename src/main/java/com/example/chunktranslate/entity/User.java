package com.example.chunktranslate.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.chunktranslate.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends BaseEntity {

    private String username;

    private String email;

    private String password;

    private String provider;

    private String providerId;

    private String avatarUrl;

    private String role;

    private Integer status;

    private LocalDateTime lastLoginAt;
}
