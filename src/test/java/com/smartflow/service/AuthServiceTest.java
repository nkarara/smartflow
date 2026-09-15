package com.smartflow.service;

import com.smartflow.dto.AuthDtos;
import com.smartflow.entity.Client;
import com.smartflow.entity.NotificationType;
import com.smartflow.entity.Role;
import com.smartflow.entity.User;
import com.smartflow.exception.BusinessException;
import com.smartflow.repository.ClientRepository;
import com.smartflow.repository.RefreshTokenRepository;
import com.smartflow.repository.SkillRepository;
import com.smartflow.repository.TechnicianRepository;
import com.smartflow.repository.UserRepository;
import com.smartflow.security.JwtProperties;
import com.smartflow.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Inscription d'un client / technicien, doublon d'email, génération des jetons.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private TechnicianRepository technicianRepository;
    @Mock
    private SkillRepository skillRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private JwtProperties jwtProperties;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AuthService authService;

    private void mockTokenGeneration() {
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken()).thenReturn("refresh-token");
        when(jwtProperties.refreshTokenValidityDays()).thenReturn(7L);
    }

    @Test
    void registerClientCreatesClientProfileAndTokens() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mockTokenGeneration();

        AuthDtos.RegisterRequest request = new AuthDtos.RegisterRequest(
                "jean@x.fr", "password123", "Jean", "Dupont", null, Role.CLIENT, "Acme SARL", "Paris", null);

        AuthDtos.AuthResponse response = authService.register(request);

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals(Role.CLIENT, response.user().role());
        verify(clientRepository).save(any(Client.class));
        verify(refreshTokenRepository).save(any(com.smartflow.entity.RefreshToken.class));
        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
    }

    @Test
    void registerWithDuplicateEmailThrowsConflict() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);

        AuthDtos.RegisterRequest request = new AuthDtos.RegisterRequest(
                "jean@x.fr", "password123", "Jean", "Dupont", null, Role.CLIENT, "Acme", null, null);

        assertThrows(BusinessException.class, () -> authService.register(request));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void registerForbidsPublicAdminSignup() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);

        AuthDtos.RegisterRequest request = new AuthDtos.RegisterRequest(
                "admin@x.fr", "password123", "Admin", "X", null, Role.ADMIN, null, null, null);

        assertThrows(BusinessException.class, () -> authService.register(request));
    }

    @Test
    void loginReturnsAccessAndRefreshTokens() {
        User user = new User();
        user.setId(7L);
        user.setEmail("admin@smartflow.fr");
        user.setFirstName("Nadia");
        user.setLastName("Admin");
        user.setRole(Role.ADMIN);
        when(userRepository.findByEmailIgnoreCase("admin@smartflow.fr")).thenReturn(java.util.Optional.of(user));
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken()).thenReturn("refresh-token");
        when(jwtProperties.refreshTokenValidityDays()).thenReturn(7L);
        when(refreshTokenRepository.save(any(com.smartflow.entity.RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthDtos.LoginRequest request = new AuthDtos.LoginRequest("admin@smartflow.fr", "Admin@123");
        AuthDtos.AuthResponse response = authService.login(request);

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals(Role.ADMIN, response.user().role());
        verify(refreshTokenRepository).save(any(com.smartflow.entity.RefreshToken.class));
    }

    @Test
    void loginWithInvalidCredentialsThrows() {
        when(authenticationManager.authenticate(
                any(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("bad"));

        AuthDtos.LoginRequest request = new AuthDtos.LoginRequest("admin@smartflow.fr", "wrong-password");
        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
                () -> authService.login(request));
        verify(refreshTokenRepository, never()).save(any());
    }
}