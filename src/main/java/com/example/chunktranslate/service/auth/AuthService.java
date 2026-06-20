package com.example.chunktranslate.service.auth;

import com.example.chunktranslate.dto.auth.AuthResponse;
import com.example.chunktranslate.dto.auth.ChangePasswordRequest;
import com.example.chunktranslate.dto.auth.LoginRequest;
import com.example.chunktranslate.dto.auth.RefreshTokenRequest;
import com.example.chunktranslate.dto.auth.RegisterRequest;
import com.example.chunktranslate.dto.auth.ResetPasswordRequest;
import com.example.chunktranslate.dto.auth.UpdateProfileRequest;
import com.example.chunktranslate.dto.auth.UserInfoResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 认证授权服务接口。
 * <p>涵盖用户注册、登录、令牌刷新、个人信息管理、邮箱验证、密码修改/重置等全部认证相关功能。</p>
 */
public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    UserInfoResponse getCurrentUser(Long userId);

    UserInfoResponse updateProfile(Long userId, UpdateProfileRequest request);

    UserInfoResponse updateAvatar(Long userId, MultipartFile file);

    void sendVerificationCode(Long userId);

    void sendRegisterCode(String email);

    void changePassword(Long userId, ChangePasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
