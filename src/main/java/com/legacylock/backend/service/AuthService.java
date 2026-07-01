package com.legacylock.backend.service;

import com.legacylock.backend.dto.request.LoginRequest;
import com.legacylock.backend.dto.request.LogoutRequest;
import com.legacylock.backend.dto.request.RefreshTokenRequest;
import com.legacylock.backend.dto.request.RegisterRequest;
import com.legacylock.backend.dto.response.AuthResponse;
import com.legacylock.backend.dto.response.RefreshTokenResponse;
import com.legacylock.backend.entity.Users;
import com.legacylock.backend.enums.AuditAction;
import com.legacylock.backend.enums.Role;
import com.legacylock.backend.exceptions.LegacyLockException;
import com.legacylock.backend.dto.request.ForgotPasswordRequest;
import com.legacylock.backend.dto.request.ResetPasswordRequest;
import com.legacylock.backend.entity.EmailVerificationToken;
import com.legacylock.backend.entity.PasswordResetToken;
import com.legacylock.backend.repository.EmailVerificationTokenRepository;
import com.legacylock.backend.repository.PasswordResetTokenRepository;
import com.legacylock.backend.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
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

import java.util.HashSet;
import java.util.Set;
import java.time.LocalDateTime;

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
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final AuthTokenService authTokenService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.email-verification.expiration-minutes:30}")
    private long emailVerificationExpirationMinutes;

    @Value("${app.password-reset.expiration-minutes:15}")
    private long passwordResetExpirationMinutes;

    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new LegacyLockException("Email already registered");
        }

        Users user = Users.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(new HashSet<>(Set.of(Role.OWNER, Role.RECEIVER)))
                .enabled(false)
                .authProvider("LOCAL")
                .providerId(null)
                .build();

        Users savedUser = userRepository.save(user);
        sendVerificationEmail(savedUser);

        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(savedUser);

        auditLogService.log(
                savedUser,
                AuditAction.USER_REGISTERED,
                "USER",
                savedUser.getId(),
                "User registered with role " + savedUser.getRoles()
        );

        return AuthResponse.builder()
                .userId(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .roles(savedUser.getRoles())
                .token(null)
                .accessToken(null)
                .refreshToken(null)
                .tokenType(null)
                .build();
    }

    public AuthResponse login(LoginRequest request) {

        Users user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new LegacyLockException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new LegacyLockException("Please verify your email before login");
        }

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
                .roles(user.getRoles())
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .build();
    }

    @Transactional
    public String verifyEmail(String rawToken) {

        String tokenHash = authTokenService.hashToken(rawToken);

        EmailVerificationToken verificationToken = emailVerificationTokenRepository
                .findByTokenHashAndUsedFalse(tokenHash)
                .orElseThrow(() -> new LegacyLockException("Invalid or expired verification token"));

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new LegacyLockException("Verification token expired");
        }

        Users user = verificationToken.getUser();

        user.setEnabled(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        verificationToken.setUsedAt(LocalDateTime.now());
        emailVerificationTokenRepository.save(verificationToken);

        auditLogService.log(
                user,
                AuditAction.EMAIL_VERIFIED,
                "USER",
                user.getId(),
                "User verified email"
        );

        return "Email verified successfully. You can now login.";
    }

    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {

        Users user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        /*
         * Do not reveal whether email exists.
         * This protects against email enumeration.
         */
        if (user == null) {
            return "If this email exists, a password reset link has been sent.";
        }

        if (!"LOCAL".equals(user.getAuthProvider())) {
            return "If this email exists, a password reset link has been sent.";
        }

        String rawToken = authTokenService.generateRawToken();
        String tokenHash = authTokenService.hashToken(rawToken);

        passwordResetTokenRepository.deleteByUser(user);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(passwordResetExpirationMinutes))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);

        String resetLink = frontendUrl + "/reset-password?token=" + rawToken;

        String subject = "LegacyLock Password Reset";

        String htmlBody = """
        <!DOCTYPE html>
        <html>
        <body style="margin:0;padding:0;background:#0f172a;font-family:Arial,sans-serif;">
            <div style="max-width:600px;margin:0 auto;padding:32px;">
                <div style="background:rgba(255,255,255,0.08);border:1px solid rgba(255,255,255,0.18);border-radius:20px;padding:32px;color:#ffffff;">
                    
                    <h1 style="margin:0 0 16px;font-size:28px;color:#ffffff;">
                        Reset your password
                    </h1>
                    
                    <p style="font-size:16px;line-height:1.6;color:#cbd5e1;">
                        Hello %s,
                    </p>
                    
                    <p style="font-size:16px;line-height:1.6;color:#cbd5e1;">
                        We received a request to reset your LegacyLock password. Click the button below to create a new password.
                    </p>
                    
                    <div style="text-align:center;margin:32px 0;">
                        <a href="%s"
                           style="display:inline-block;background:linear-gradient(135deg,#f97316,#ec4899);color:#ffffff;text-decoration:none;padding:14px 28px;border-radius:999px;font-weight:bold;font-size:16px;">
                            Reset Password
                        </a>
                    </div>
                    
                    <p style="font-size:14px;line-height:1.6;color:#94a3b8;">
                        This link will expire in %d minutes.
                    </p>
                    
                    <p style="font-size:14px;line-height:1.6;color:#94a3b8;">
                        If you did not request this, you can safely ignore this email.
                    </p>
                    
                    <p style="font-size:14px;line-height:1.6;color:#94a3b8;">
                        If the button does not work, copy and paste this link into your browser:
                    </p>
                    
                    <p style="word-break:break-all;font-size:13px;color:#38bdf8;">
                        %s
                    </p>
                    
                    <hr style="border:none;border-top:1px solid rgba(255,255,255,0.12);margin:24px 0;">
                    
                    <p style="font-size:13px;color:#64748b;">
                        Regards,<br>
                        LegacyLock Team
                    </p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(
                user.getName(),
                resetLink,
                passwordResetExpirationMinutes,
                resetLink
        );

        emailService.sendHtmlEmail(user.getEmail(), subject, htmlBody);

        auditLogService.log(
                user,
                AuditAction.PASSWORD_RESET_EMAIL_SENT,
                "USER",
                user.getId(),
                "Password reset email sent"
        );

        return "If this email exists, a password reset link has been sent.";
    }

    @Transactional
    public String resetPassword(ResetPasswordRequest request) {

        if (request.getNewPassword() == null || request.getNewPassword().length() < 8) {
            throw new LegacyLockException("Password must be at least 8 characters long");
        }

        String tokenHash = authTokenService.hashToken(request.getToken());

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenHashAndUsedFalse(tokenHash)
                .orElseThrow(() -> new LegacyLockException("Invalid or expired reset token"));

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new LegacyLockException("Reset token expired");
        }

        Users user = resetToken.getUser();

        if (!"LOCAL".equals(user.getAuthProvider())) {
            throw new LegacyLockException("Password reset is only available for email/password accounts");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);

        refreshTokenRepository.deleteByUser(user);

        auditLogService.log(
                user,
                AuditAction.PASSWORD_RESET_COMPLETED,
                "USER",
                user.getId(),
                "Password reset completed"
        );

        return "Password reset successful. You can now login with your new password.";
    }

    private void sendVerificationEmail(Users user) {

        emailVerificationTokenRepository.deleteByUser(user);

        String rawToken = authTokenService.generateRawToken();
        String tokenHash = authTokenService.hashToken(rawToken);

        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(emailVerificationExpirationMinutes))
                .used(false)
                .build();

        emailVerificationTokenRepository.save(verificationToken);

        String verificationLink = frontendUrl + "/verify-email?token=" + rawToken;

        String subject = "Verify your LegacyLock email";

        String htmlBody = """
        <!DOCTYPE html>
        <html>
        <body style="margin:0;padding:0;background:#0f172a;font-family:Arial,sans-serif;">
            <div style="max-width:600px;margin:0 auto;padding:32px;">
                <div style="background:rgba(255,255,255,0.08);border:1px solid rgba(255,255,255,0.18);border-radius:20px;padding:32px;color:#ffffff;">
                    
                    <h1 style="margin:0 0 16px;font-size:28px;color:#ffffff;">
                        Verify your email
                    </h1>
                    
                    <p style="font-size:16px;line-height:1.6;color:#cbd5e1;">
                        Hello %s,
                    </p>
                    
                    <p style="font-size:16px;line-height:1.6;color:#cbd5e1;">
                        Welcome to <strong>LegacyLock</strong>. Please verify your email address to activate your account.
                    </p>
                    
                    <div style="text-align:center;margin:32px 0;">
                        <a href="%s"
                           style="display:inline-block;background:linear-gradient(135deg,#38bdf8,#6366f1);color:#ffffff;text-decoration:none;padding:14px 28px;border-radius:999px;font-weight:bold;font-size:16px;">
                            Verify Email
                        </a>
                    </div>
                    
                    <p style="font-size:14px;line-height:1.6;color:#94a3b8;">
                        This link will expire in %d minutes.
                    </p>
                    
                    <p style="font-size:14px;line-height:1.6;color:#94a3b8;">
                        If the button does not work, copy and paste this link into your browser:
                    </p>
                    
                    <p style="word-break:break-all;font-size:13px;color:#38bdf8;">
                        %s
                    </p>
                    
                    <hr style="border:none;border-top:1px solid rgba(255,255,255,0.12);margin:24px 0;">
                    
                    <p style="font-size:13px;color:#64748b;">
                        Regards,<br>
                        LegacyLock Team
                    </p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(
                user.getName(),
                verificationLink,
                emailVerificationExpirationMinutes,
                verificationLink
        );

        emailService.sendHtmlEmail(user.getEmail(), subject, htmlBody);

        auditLogService.log(
                user,
                AuditAction.EMAIL_VERIFICATION_SENT,
                "USER",
                user.getId(),
                "Email verification link sent"
        );
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
                .roles(user.getRoles())
                .token(accessToken)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
    }
}
