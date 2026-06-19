package com.example.chunktranslate.security;

import com.example.chunktranslate.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

/**
 * 同时实现 OAuth2User 和 UserDetails，满足 OAuth2 认证链和后续 JWT 鉴权的双重需求
 */
public class CustomOAuth2User extends CustomUserDetails implements OAuth2User {

    private final Map<String, Object> attributes;

    public CustomOAuth2User(User user, Map<String, Object> attributes) {
        super(user);
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return super.getAuthorities();
    }

    @Override
    public String getName() {
        // GitHub OAuth2 的 user-name-attribute = "id"（数字 ID）
        return String.valueOf(attributes.get("id"));
    }
}
