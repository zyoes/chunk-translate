package com.example.chunktranslate.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.chunktranslate.common.enums.ProviderType;
import com.example.chunktranslate.entity.User;
import com.example.chunktranslate.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 自定义用户详情服务。
 * <p>实现 Spring Security 的 {@link UserDetailsService}，支持按用户名/邮箱加载本地用户，
 * 以及按用户 ID 加载（供 JWT 过滤器使用）。</p>
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    /**
     * 按 username 或 email 加载本地用户（供邮箱登录认证）
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getProvider, ProviderType.LOCAL.getValue())
                        .and(w -> w.eq(User::getUsername, username).or().eq(User::getEmail, username))
        );
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        return new CustomUserDetails(user);
    }

    /**
     * 按 userId 加载用户（供 JwtAuthenticationFilter 从 token 恢复身份）
     */
    public CustomUserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: id=" + userId);
        }
        return new CustomUserDetails(user);
    }
}
