package com.nguyenquyen.vetautet.ddd.application.service.auth.impl;

import com.nguyenquyen.vetautet.ddd.application.model.response.AuthResponse;
import com.nguyenquyen.vetautet.ddd.application.service.auth.AuthAppService;
import com.nguyenquyen.vetautet.ddd.domain.model.entity.User;
import com.nguyenquyen.vetautet.ddd.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthAppServiceImpl implements AuthAppService {

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthResponse login(String email, String password) {
        // Authenticate via Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found after auth"));

        String scope = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

        return generateTokens(user, scope);
    }

    @Value("${app.google.client-id}")
    private String googleClientId;

    @Override
    public AuthResponse googleLogin(String idTokenString) {
        try {
            // Verify Google ID Token
            com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier verifier =
                    new com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier.Builder(
                            new com.google.api.client.http.javanet.NetHttpTransport(),
                            new com.google.api.client.json.gson.GsonFactory())
                            .setAudience(java.util.Collections.singletonList(googleClientId))
                            .build();

            com.google.api.client.googleapis.auth.oauth2.GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new RuntimeException("Invalid Google ID Token");
            }

            com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String subjectId = payload.getSubject();
            String pictureUrl = (String) payload.get("picture");

            // Find or Create User
            User user = userRepository.findByEmail(email).orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setRole("ROLE_USER");
                return newUser;
            });

            // Update Social Info
            user.setSocialId(subjectId);
            if (pictureUrl != null) {
                user.setAvatarUrl(pictureUrl);
            }
            userRepository.save(user); // Save either new or updated user

            return generateTokens(user, user.getRole());

        } catch (Exception e) {
            log.error("Google login failed", e);
            throw new RuntimeException("Google login failed: " + e.getMessage());
        }
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        try {
            // Verify refresh token
            Jwt jwt = jwtDecoder.decode(refreshToken);
            
            // Check token type if necessary, or just rely on expiry and subject
            String type = jwt.getClaimAsString("type");
            if (!"REFRESH".equals(type)) {
                throw new RuntimeException("Invalid token type");
            }
            
            String email = jwt.getSubject();
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            return generateTokens(user, user.getRole());
        } catch (JwtException e) {
            log.error("Refresh token error", e);
            throw new RuntimeException("Invalid or expired refresh token");
        }
    }

    private AuthResponse generateTokens(User user, String scope) {
        Instant now = Instant.now();
        
        // JwsHeader is required to tell the encoder which algorithm to use (HS256)
        JwsHeader jwsHeader = JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build();

        // Access Token (1 hour)
        JwtClaimsSet accessClaims = JwtClaimsSet.builder()
                .issuer("vetautet-auth")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600)) // 1 hour
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("scope", scope)
                .claim("type", "ACCESS")
                .build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, accessClaims)).getTokenValue();

        // Refresh Token (30 days)
        JwtClaimsSet refreshClaims = JwtClaimsSet.builder()
                .issuer("vetautet-auth")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(30 * 24 * 3600)) // 30 days
                .subject(user.getEmail())
                .claim("type", "REFRESH")
                .build();
        String refreshTokenStr = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, refreshClaims)).getTokenValue();

        return new AuthResponse(accessToken, refreshTokenStr, user.getEmail(), user.getRole(), user.getAvatarUrl());
    }

    @Override
    public void register(String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email is already taken");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("ROLE_USER");

        userRepository.save(user);
        log.info("Registered new user with email: {}", email);
    }
}
