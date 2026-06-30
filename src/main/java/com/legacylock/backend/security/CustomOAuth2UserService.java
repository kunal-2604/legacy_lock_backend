package com.legacylock.backend.security;

import com.legacylock.backend.entity.Users;
import com.legacylock.backend.enums.AuditAction;
import com.legacylock.backend.enums.Role;
import com.legacylock.backend.repository.UserRepository;
import com.legacylock.backend.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {

        OAuth2User oauth2User = super.loadUser(userRequest);

        String provider = userRequest
                .getClientRegistration()
                .getRegistrationId()
                .toUpperCase();

        String providerId = oauth2User.getAttribute("sub");
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        Users user = userRepository.findByEmail(email)
                .map(existingUser -> updateExistingUser(existingUser, provider, providerId, name))
                .orElseGet(() -> createNewOAuthUser(name, email, provider, providerId));

        return oauth2User;
    }

    private Users createNewOAuthUser(
            String name,
            String email,
            String provider,
            String providerId
    ) {
        Users user = Users.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode("OAUTH2_USER_NO_LOCAL_PASSWORD"))
                .role(Role.OWNER)
                .enabled(true)
                .authProvider(provider)
                .providerId(providerId)
                .build();

        Users savedUser = userRepository.save(user);

        auditLogService.log(
                savedUser,
                AuditAction.USER_REGISTERED,
                "USER",
                savedUser.getId(),
                "User registered using " + provider + " OAuth2"
        );

        return savedUser;
    }

    private Users updateExistingUser(
            Users user,
            String provider,
            String providerId,
            String name
    ) {
        user.setName(name);
        user.setAuthProvider(provider);
        user.setProviderId(providerId);
        user.setEnabled(true);

        return userRepository.save(user);
    }
}
