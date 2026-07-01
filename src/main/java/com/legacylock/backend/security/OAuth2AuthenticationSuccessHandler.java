package com.legacylock.backend.security;

import com.legacylock.backend.entity.Users;
import com.legacylock.backend.enums.AuditAction;
import com.legacylock.backend.enums.Role;
import com.legacylock.backend.repository.UserRepository;
import com.legacylock.backend.service.AuditLogService;
import com.legacylock.backend.service.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashSet;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsService userDetailsService;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.oauth2.redirect-url}")
    private String frontendRedirectUrl;

    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String providerId = oauth2User.getAttribute("sub");

        if (email == null || email.isBlank()) {
            throw new RuntimeException("OAuth2 email not found");
        }

        Users user = userRepository.findByEmail(email)
                .orElseGet(() -> createOAuth2User(name, email, providerId));

        updateOAuth2User(user, name, providerId);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        auditLogService.log(
                user,
                AuditAction.USER_LOGGED_IN,
                "USER",
                user.getId(),
                "User logged in using GOOGLE OAuth2"
        );

        String roles = user.getRoles()
                .stream()
                .map(Role::name)
                .collect(Collectors.joining(","));

        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendRedirectUrl)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .queryParam("roles", roles)
                .queryParam("email", user.getEmail())
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }

    private Users createOAuth2User(String name, String email, String providerId) {

        Users user = Users.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode("OAUTH2_USER_NO_LOCAL_PASSWORD"))
                .roles(new HashSet<>(Set.of(Role.OWNER, Role.RECEIVER)))
                .enabled(true)
                .authProvider("GOOGLE")
                .providerId(providerId)
                .build();

        Users savedUser = userRepository.save(user);

        auditLogService.log(
                savedUser,
                AuditAction.USER_REGISTERED,
                "USER",
                savedUser.getId(),
                "User registered using GOOGLE OAuth2"
        );

        return savedUser;
    }

    private void updateOAuth2User(Users user, String name, String providerId) {
        user.setName(name);
        user.setAuthProvider("GOOGLE");
        user.setProviderId(providerId);
        user.setEnabled(true);

        Set<Role> roles = user.getRoles();

        if (roles == null) {
            roles = new HashSet<>();
        } else {
            roles = new HashSet<>(roles);
        }

        roles.add(Role.OWNER);
        roles.add(Role.RECEIVER);

        user.setRoles(roles);

        userRepository.save(user);
    }
}
