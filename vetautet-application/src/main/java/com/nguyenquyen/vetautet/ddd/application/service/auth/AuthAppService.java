package com.nguyenquyen.vetautet.ddd.application.service.auth;

import com.nguyenquyen.vetautet.ddd.application.model.response.AuthResponse;

public interface AuthAppService {
    AuthResponse login(String email, String password);
    AuthResponse googleLogin(String idToken);
    AuthResponse refreshToken(String refreshToken);
    void register(String email, String password);
}
