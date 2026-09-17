package com.smartflow.controller;

import com.smartflow.dto.AuthDtos;
import com.smartflow.dto.UserDtos;
import com.smartflow.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inscription, connexion, renouvellement de session et profil courant.
 * Seul endpoint public de l'API (voir {@code SecurityConfig}, {@code /api/auth/**}).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    /** Service métier d'authentification. */
    private final AuthService authService;

    /** Inscription publique (CLIENT ou TECHNICIAN uniquement) → crée le compte et émet les jetons. */
    @PostMapping("/register")
    public AuthDtos.AuthResponse register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return authService.register(request);
    }

    /** Connexion (email + mot de passe) → émet access/refresh tokens. */
    @PostMapping("/login")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return authService.login(request);
    }

    /** Renouvelle les jetons à partir du refresh token (rotation). */
    @PostMapping("/refresh")
    public AuthDtos.AuthResponse refresh(@Valid @RequestBody AuthDtos.RefreshRequest request) {
        return authService.refresh(request);
    }

    /** Déconnexion : révoque le refresh token (le client peut ne rien envoyer). */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) AuthDtos.RefreshRequest request) {
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            authService.logout(request.refreshToken());
        }
        return ResponseEntity.noContent().build();
    }

    /** Profil complet de l'utilisateur actuellement connecté (jeton requis). */
    @GetMapping("/me")
    public UserDtos.UserResponse me() {
        return authService.me();
    }
}