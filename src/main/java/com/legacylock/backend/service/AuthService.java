package com.legacylock.backend.service;

import com.legacylock.backend.dto.request.LoginRequest;
import com.legacylock.backend.dto.request.LogoutRequest;
import com.legacylock.backend.dto.request.RefreshTokenRequest;
import com.legacylock.backend.dto.request.RegisterRequest;
import com.legacylock.backend.dto.response.AuthResponse;
import com.legacylock.backend.dto.response.RefreshTokenResponse;
import com.legacylock.backend.entity.Users;
import com.legacylock.backend.enums.AuditAction;
import com.legacylock.backend.exceptions.LegacyLockException;
import com.legacylock.backend.repository.UserRepository;
import com.legacylock.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager;
    private final AuditLogService auditLogService;
    private final RefreshTokenService refreshTokenService;

    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new LegacyLockException("Email already registered");
        }

        Users user = Users.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .enabled(true)
                .authProvider("LOCAL")
                .providerId(null)
                .build();

        Users savedUser = userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(savedUser);

        auditLogService.log(
                savedUser,
                AuditAction.USER_REGISTERED,
                "USER",
                savedUser.getId(),
                "User registered with role " + savedUser.getRole()
        );

        return buildAuthResponse(savedUser, accessToken, refreshToken);
    }

    public AuthResponse login(LoginRequest request) {

        Users user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new LegacyLockException("Invalid email or password"));

        if (!"LOCAL".equals(user.getAuthProvider())) {
            throw new LegacyLockException("Please login using " + user.getAuthProvider());
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        auditLogService.log(
                user,
                AuditAction.USER_LOGGED_IN,
                "USER",
                user.getId(),
                "User logged in"
        );

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    public RefreshTokenResponse refresh(RefreshTokenRequest request) {

        Users user = refreshTokenService.getUserFromRefreshToken(request.getRefreshToken());

        String newRefreshToken = refreshTokenService.rotateRefreshToken(request.getRefreshToken());

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String newAccessToken = jwtService.generateToken(userDetails);

        auditLogService.log(
                user,
                AuditAction.TOKEN_REFRESHED,
                "USER",
                user.getId(),
                "Access token refreshed"
        );

        return RefreshTokenResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .build();
    }

    public void logout(LogoutRequest request) {

        Users user = refreshTokenService.getUserFromRefreshToken(request.getRefreshToken());

        refreshTokenService.revokeRefreshToken(request.getRefreshToken());

        auditLogService.log(
                user,
                AuditAction.USER_LOGGED_OUT,
                "USER",
                user.getId(),
                "User logged out"
        );
    }

    private AuthResponse buildAuthResponse(Users user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .token(accessToken)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
    }
}
