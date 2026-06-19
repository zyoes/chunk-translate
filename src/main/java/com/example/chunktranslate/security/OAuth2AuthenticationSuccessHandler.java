package com.example.chunktranslate.security;

import com.example.chunktranslate.entity.RefreshToken;
import com.example.chunktranslate.entity.User;
import com.example.chunktranslate.mapper.RefreshTokenMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenMapper refreshTokenMapper;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response, Authentication authentication) throws IOException {

        CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();
        User user = principal.getUser();

        // 签发 JWT
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken();

        // 存储 refresh token 到 DB
        RefreshToken rt = new RefreshToken();
        rt.setUserId(user.getId());
        rt.setToken(refreshToken);
        rt.setExpiresAt(LocalDateTime.now().plusDays(7));
        rt.setRevoked(0);
        refreshTokenMapper.insert(rt);

        log.info("OAuth2 登录成功: userId={}, username={}", user.getId(), user.getUsername());

        // 302 重定向到前端回调页，token 通过 URL query 参数传递
        String redirectUrl = String.format(
                "%s/oauth/callback?accessToken=%s&refreshToken=%s",
                frontendUrl, accessToken, refreshToken);
        response.sendRedirect(redirectUrl);
    }
}
