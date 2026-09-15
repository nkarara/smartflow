package com.smartflow.service;

import com.smartflow.dto.AuthDtos;
import com.smartflow.dto.UserDtos;
import com.smartflow.entity.Client;
import com.smartflow.entity.NotificationType;
import com.smartflow.entity.RefreshToken;
import com.smartflow.entity.Role;
import com.smartflow.entity.Skill;
import com.smartflow.entity.Technician;
import com.smartflow.entity.User;
import com.smartflow.exception.BusinessException;
import com.smartflow.mapper.UserMapper;
import com.smartflow.repository.ClientRepository;
import com.smartflow.repository.RefreshTokenRepository;
import com.smartflow.repository.SkillRepository;
import com.smartflow.repository.TechnicianRepository;
import com.smartflow.repository.UserRepository;
import com.smartflow.security.JwtProperties;
import com.smartflow.security.JwtService;
import com.smartflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Inscription, connexion, refresh token et déconnexion.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final TechnicianRepository technicianRepository;
    private final SkillRepository skillRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;
    private final NotificationService notificationService;

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Un compte existe déjà avec cet email");
        }
        if (request.role() != Role.CLIENT && request.role() != Role.TECHNICIAN) {
            throw new BusinessException("L'inscription publique est réservée aux clients et aux techniciens");
        }

        User user = User.builder()
                .email(request.email().trim().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .role(request.role())
                .enabled(true)
                .build();
        user = userRepository.save(user);

        if (request.role() == Role.CLIENT) {
            clientRepository.save(Client.builder()
                    .user(user)
                    .companyName(request.companyName())
                    .city(request.location())
                    .build());
        } else {
            Technician technician = Technician.builder()
                    .user(user)
                    .location(request.location())
                    .available(true)
                    .build();
            if (request.skills() != null) {
                request.skills().forEach(name -> technician.getSkills().add(findOrCreateSkill(name)));
            }
            technicianRepository.save(technician);
        }
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe invalide"));
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthDtos.AuthResponse refresh(AuthDtos.RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Session invalide"));
        if (!stored.isValid()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Session expirée, veuillez vous reconnecter");
        }
        User user = stored.getUser();
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    public UserDtos.UserResponse me() {
        User user = SecurityUtils.currentUser();
        Long clientId = clientRepository.findByUserId(user.getId()).map(Client::getId).orElse(null);
        Long technicianId = technicianRepository.findByUserId(user.getId()).map(Technician::getId).orElse(null);
        return UserMapper.toUserResponse(user, clientId, technicianId);
    }

    private AuthDtos.AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = RefreshToken.builder()
                .token(jwtService.generateRefreshToken())
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(jwtProperties.refreshTokenValidityDays()))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return new AuthDtos.AuthResponse(
                accessToken,
                refreshToken.getToken(),
                new AuthDtos.AuthUser(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole())
        );
    }

    private Skill findOrCreateSkill(String name) {
        String trimmed = name.trim();
        return skillRepository.findByNameIgnoreCase(trimmed)
                .orElseGet(() -> skillRepository.save(Skill.builder().name(trimmed).build()));
    }
}