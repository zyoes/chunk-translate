package com.example.chunktranslate.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.chunktranslate.common.enums.ProviderType;
import com.example.chunktranslate.common.enums.UserRole;
import com.example.chunktranslate.entity.User;
import com.example.chunktranslate.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserMapper userMapper;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 委托 DefaultOAuth2UserService 获取 GitHub 用户信息
        OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String providerId = String.valueOf(attributes.get("id"));
        String username = (String) attributes.get("login");
        String email = (String) attributes.get("email");
        String avatarUrl = (String) attributes.get("avatar_url");

        // 按 provider + providerId 查找已有用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getProvider, registrationId)
                        .eq(User::getProviderId, providerId)
        );

        if (user == null) {
            // 首次 GitHub 登录，创建本地用户
            user = new User();
            user.setUsername(registrationId + "_" + username);
            user.setEmail(email);
            user.setProvider(ProviderType.GITHUB.getValue());
            user.setProviderId(providerId);
            user.setAvatarUrl(avatarUrl);
            user.setRole(UserRole.USER.getValue());
            user.setStatus(1);
            user.setLastLoginAt(LocalDateTime.now());
            userMapper.insert(user);
            log.info("GitHub 新用户注册: username={}, providerId={}", user.getUsername(), providerId);
        } else {
            // 已有用户，更新头像和登录时间
            user.setAvatarUrl(avatarUrl);
            user.setLastLoginAt(LocalDateTime.now());
            userMapper.updateById(user);
            log.info("GitHub 用户登录: userId={}, username={}", user.getId(), user.getUsername());
        }

        return new CustomOAuth2User(user, attributes);
    }
}
