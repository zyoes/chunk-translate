package com.example.chunktranslate.config;

import com.example.chunktranslate.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.Customizer;

/**
 * Spring Security 安全配置。
 * <p>核心策略：</p>
 * <ul>
 *   <li>无状态 JWT 认证（禁用 CSRF、不使用 Session）</li>
 *   <li>公开接口（注册/登录/文档查看/翻译/导出/Swagger）无需登录</li>
 *   <li>用户信息接口（/api/auth/me、/api/auth/profile 等）需要认证</li>
 *   <li>GitHub OAuth2 登录支持</li>
 *   <li>JWT 过滤器在 UsernamePasswordAuthenticationFilter 之前执行</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 公开接口（无需登录）
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/send-register-code",
                                "/api/auth/reset-password"
                        ).permitAll()
                        .requestMatchers("/oauth2/**", "/login/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers(
                                "/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs", "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers("/api/document/**",
                                "/api/translation/start",
                                "/api/translation/progress/**",
                                "/api/translation/stop/**",
                                "/api/translation/chunk/**",
                                "/api/export/**"
                        ).permitAll()
                        // 需登录接口
                        .requestMatchers("/api/auth/me",
                                "/api/auth/profile",
                                "/api/auth/avatar",
                                "/api/auth/send-code",
                                "/api/auth/change-password",
                                "/api/translation/history",
                                "/api/translation/history/**"
                        ).authenticated()
                        // 管理员接口
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(ui -> ui.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                )
                .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, customUserDetailsService);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
