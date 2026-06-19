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
