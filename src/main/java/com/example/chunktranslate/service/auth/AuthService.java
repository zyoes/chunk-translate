package com.example.chunktranslate.service.auth;

import com.example.chunktranslate.dto.auth.AuthResponse;
import com.example.chunktranslate.dto.auth.LoginRequest;
import com.example.chunktranslate.dto.auth.RefreshTokenRequest;
import com.example.chunktranslate.dto.auth.RegisterRequest;
import com.example.chunktranslate.dto.auth.UserInfoResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    UserInfoResponse getCurrentUser(Long userId);
}
