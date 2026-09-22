package com.nguyenquyen.vetautet.ddd.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public class SecurityUtils {

    /**
     * Lấy User ID của người dùng hiện tại từ JWT token
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            // Lấy từ claim "userId"
            Object userId = jwt.getClaim("userId");
            if (userId instanceof Long) {
                return (Long) userId;
            } else if (userId instanceof Integer) {
                return ((Integer) userId).longValue();
            } else if (userId instanceof String) {
                return Long.parseLong((String) userId);
            }
        }
        return null;
    }

    /**
     * Lấy Email (subject) của người dùng hiện tại từ JWT token
     */
    public static String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            return jwt.getClaimAsString("sub");
        }
        return null;
    }
}
