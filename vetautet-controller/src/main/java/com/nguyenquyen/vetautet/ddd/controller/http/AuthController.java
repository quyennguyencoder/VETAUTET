package com.nguyenquyen.vetautet.ddd.controller.http;

import com.nguyenquyen.vetautet.ddd.application.model.response.AuthResponse;
import com.nguyenquyen.vetautet.ddd.application.service.auth.AuthAppService;
import com.nguyenquyen.vetautet.ddd.controller.dto.LoginRequest;
import com.nguyenquyen.vetautet.ddd.controller.dto.RefreshTokenRequest;
import com.nguyenquyen.vetautet.ddd.controller.dto.RegisterRequest;
import com.nguyenquyen.vetautet.ddd.controller.model.enums.ResultUtil;
import com.nguyenquyen.vetautet.ddd.controller.model.vo.ResultMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthAppService authAppService;

    @PostMapping("/login")
    public ResultMessage<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("REST request to login with email: {}", request.getEmail());
        try {
            AuthResponse response = authAppService.login(request.getEmail(), request.getPassword());
            return ResultUtil.data(response);
        } catch (Exception e) {
            log.error("Login failed for email: {}", request.getEmail(), e);
            return ResultUtil.data(null);
        }
    }

    @PostMapping("/google")
    public ResultMessage<AuthResponse> googleLogin(@Valid @RequestBody com.nguyenquyen.vetautet.ddd.controller.dto.GoogleLoginRequest request) {
        log.info("REST request to login with Google");
        try {
            AuthResponse response = authAppService.googleLogin(request.getIdToken());
            return ResultUtil.data(response);
        } catch (Exception e) {
            log.error("Google login failed", e);
            return ResultUtil.data(null);
        }
    }

    @PostMapping("/refresh")
    public ResultMessage<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("REST request to refresh token");
        try {
            AuthResponse response = authAppService.refreshToken(request.getRefreshToken());
            return ResultUtil.data(response);
        } catch (Exception e) {
            log.error("Refresh token failed", e);
            return ResultUtil.data(null);
        }
    }

    @PostMapping("/register")
    public ResultMessage<String> register(@Valid @RequestBody RegisterRequest request) {
        log.info("REST request to register email: {}", request.getEmail());
        try {
            authAppService.register(request.getEmail(), request.getPassword());
            return ResultUtil.data("User registered successfully");
        } catch (Exception e) {
            log.error("Registration failed for email: {}", request.getEmail(), e);
            return ResultUtil.data("Failed to register: " + e.getMessage());
        }
    }
}

