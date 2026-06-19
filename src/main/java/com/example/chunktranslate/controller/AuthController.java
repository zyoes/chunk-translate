package com.example.chunktranslate.controller;

import com.example.chunktranslate.common.result.Result;
import com.example.chunktranslate.dto.auth.AuthResponse;
import com.example.chunktranslate.dto.auth.LoginRequest;
import com.example.chunktranslate.dto.auth.RefreshTokenRequest;
import com.example.chunktranslate.dto.auth.RegisterRequest;
import com.example.chunktranslate.dto.auth.UserInfoResponse;
import com.example.chunktranslate.security.UserContext;
import com.example.chunktranslate.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户认证", description = "注册、登录、令牌刷新、当前用户信息")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "邮箱注册")
    @PostMapping("/register")
    public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @Operation(summary = "邮箱登录")
    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public Result<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.success(authService.refresh(request));
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<UserInfoResponse> me() {
        Long userId = UserContext.getUserId();
        return Result.success(authService.getCurrentUser(userId));
    }
}
