package com.example.chunktranslate.service.auth.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.chunktranslate.common.enums.ProviderType;
import com.example.chunktranslate.common.enums.UserRole;
import com.example.chunktranslate.common.exception.BusinessException;
import com.example.chunktranslate.common.result.ResultCode;
import com.example.chunktranslate.dto.auth.AuthResponse;
import com.example.chunktranslate.dto.auth.LoginRequest;
import com.example.chunktranslate.dto.auth.RefreshTokenRequest;
import com.example.chunktranslate.dto.auth.RegisterRequest;
import com.example.chunktranslate.dto.auth.UpdateProfileRequest;
import com.example.chunktranslate.dto.auth.UserInfoResponse;
import com.example.chunktranslate.entity.RefreshToken;
import com.example.chunktranslate.entity.User;
import com.example.chunktranslate.mapper.RefreshTokenMapper;
import com.example.chunktranslate.mapper.UserMapper;
import com.example.chunktranslate.security.JwtTokenProvider;
import com.example.chunktranslate.security.UserContext;
import com.example.chunktranslate.service.auth.AuthService;
import com.example.chunktranslate.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageUtil fileStorageUtil;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 校验用户名唯一性
        if (userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())) != null) {
            throw new BusinessException(ResultCode.USERNAME_ALREADY_EXISTS);
        }
        // 校验邮箱唯一性（仅查本地注册用户，GitHub 用户允许同邮箱独立账号）
        if (userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, request.getEmail())
                .eq(User::getProvider, ProviderType.LOCAL.getValue())) != null) {
            throw new BusinessException(ResultCode.EMAIL_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setProvider(ProviderType.LOCAL.getValue());
        user.setRole(UserRole.USER.getValue());
        user.setStatus(1);
        userMapper.insert(user);

        log.info("用户注册成功: userId={}, username={}", user.getId(), user.getUsername());
        return generateAuthResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getProvider, ProviderType.LOCAL.getValue())
                        .and(w -> w.eq(User::getUsername, request.getUsername())
                                .or().eq(User::getEmail, request.getUsername()))
        );
        if (user == null) {
            throw new BusinessException(ResultCode.INVALID_CREDENTIALS);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.INVALID_CREDENTIALS);
        }

        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());
        return generateAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken rt = refreshTokenMapper.selectOne(
                new LambdaQueryWrapper<RefreshToken>()
                        .eq(RefreshToken::getToken, request.getRefreshToken())
        );
        if (rt == null) {
            throw new BusinessException(ResultCode.INVALID_TOKEN);
        }
        if (rt.getRevoked() == 1) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_REVOKED);
        }
        if (rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_EXPIRED);
        }

        // 撤销旧的 refresh token
        rt.setRevoked(1);
        refreshTokenMapper.updateById(rt);

        // 签发新 token
        User user = userMapper.selectById(rt.getUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        log.info("令牌刷新成功: userId={}", user.getId());
        return generateAuthResponse(user);
    }

    @Override
    public UserInfoResponse getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        UserInfoResponse resp = new UserInfoResponse();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setEmail(user.getEmail());
        resp.setAvatarUrl(user.getAvatarUrl());
        resp.setRole(user.getRole());
        return resp;
    }

    @Override
    public UserInfoResponse updateProfile(Long userId, UpdateProfileRequest request) {
        // 验证只能修改自己的资料
        Long currentUserId = UserContext.getUserId();
        if (currentUserId == null || !currentUserId.equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            User existing = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, request.getUsername())
                    .ne(User::getId, userId));
            if (existing != null) {
                throw new BusinessException(ResultCode.USERNAME_ALREADY_EXISTS);
            }
            user.setUsername(request.getUsername());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        userMapper.updateById(user);
        log.info("用户资料更新: userId={}, username={}", userId, user.getUsername());
        return getCurrentUser(userId);
    }

    @Override
    public UserInfoResponse updateAvatar(Long userId, MultipartFile file) {
        Long currentUserId = UserContext.getUserId();
        if (currentUserId == null || !currentUserId.equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        String path = fileStorageUtil.store(file);
        user.setAvatarUrl("/uploads/" + path);
        userMapper.updateById(user);
        log.info("用户头像更新: userId={}, avatar={}", userId, path);
        return getCurrentUser(userId);
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshTokenStr = jwtTokenProvider.generateRefreshToken();

        RefreshToken rt = new RefreshToken();
        rt.setUserId(user.getId());
        rt.setToken(refreshTokenStr);
        rt.setExpiresAt(LocalDateTime.now().plusDays(7));
        rt.setRevoked(0);
        refreshTokenMapper.insert(rt);

        UserInfoResponse userInfo = new UserInfoResponse();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setEmail(user.getEmail());
        userInfo.setAvatarUrl(user.getAvatarUrl());
        userInfo.setRole(user.getRole());

        AuthResponse resp = new AuthResponse();
        resp.setAccessToken(accessToken);
        resp.setRefreshToken(refreshTokenStr);
        resp.setExpiresIn(900);
        resp.setUser(userInfo);
        return resp;
    }
}
