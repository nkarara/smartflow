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
 * Inscription, connexion, renouvellement de session (refresh) et déconnexion.
 *
 * <p>Fonctionnement résumé :
 * <ul>
 *   <li>l'<b>inscription</b> crée un {@link User} + son profil métier
 *       ({@link Client} ou {@link Technician}) ;</li>
 *   <li>la <b>connexion</b> délègue la vérification email/mot de passe à
 *       l'{@code AuthenticationManager} (BCrypt) ;</li>
 *   <li>chaque succès émet un <b>access token</b> (JWT) et un <b>refresh token</b>
 *       (UUID stocké en base) ;</li>
 *   <li>le <b>refresh</b> invalide l'ancien jeton (rotation) et en émet un nouveau.</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    /** Dépôt d'accès aux comptes utilisateurs. */
    private final UserRepository userRepository;
    /** Dépôt d'accès aux profils clients. */
    private final ClientRepository clientRepository;
    /** Dépôt d'accès aux profils techniciens. */
    private final TechnicianRepository technicianRepository;
    /** Dépôt d'accès aux compétences. */
    private final SkillRepository skillRepository;
    /** Dépôt d'accès aux refresh tokens. */
    private final RefreshTokenRepository refreshTokenRepository;
    /** Encodeur BCrypt pour hacher les mots de passe. */
    private final PasswordEncoder passwordEncoder;
    /** Générateur de jetons JWT / refresh. */
    private final JwtService jwtService;
    /** Durées de validité des jetons (configuration). */
    private final JwtProperties jwtProperties;
    /** Manager Spring Security : valide les identifiants à la connexion. */
    private final AuthenticationManager authenticationManager;
    /** Service d'envoi de notifications. */
    private final NotificationService notificationService;

    /**
     * Inscription publique : seule les rôles CLIENT et TECHNICIAN sont autorisés.
     *
     * @param request données d'inscription (email, mot de passe, profil)
     * @return les jetons (access + refresh) + le profil utilisateur créé
     */
    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        // 1. Contrôle d'unicité : un email ne peut être utilisé qu'une seule fois
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Un compte existe déjà avec cet email");
        }
        // 2. L'inscription publique est réservée aux clients et techniciens
        if (request.role() != Role.CLIENT && request.role() != Role.TECHNICIAN) {
            throw new BusinessException("L'inscription publique est réservée aux clients et aux techniciens");
        }

        // 3. Création du compte : email normalisé (minuscules), mot de passe haché BCrypt
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

        // 4. Profil métier selon le rôle choisi
        if (request.role() == Role.CLIENT) {
            // Un client = une éventuelle société + sa ville
            clientRepository.save(Client.builder()
                    .user(user)
                    .companyName(request.companyName())
                    .city(request.location())
                    .build());
        } else {
            // Un technicien = un profil avec ville + compétences (créées si absentes)
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
        // 5. L'utilisateur inscrit est directement connecté (jetons émis)
        return buildAuthResponse(user);
    }

    /**
     * Connexion : vérifie les identifiants puis émet les jetons.
     *
     * @param request identifiants (email + mot de passe)
     * @return les jetons (access + refresh) + le profil utilisateur
     */
    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        // Délégation à Spring Security : charge l'utilisateur et vérifie le BCrypt.
        // En cas d'échec, BadCredentialsException est levée (→ 401 au niveau API).
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        // Identifiants valides : on recharge l'utilisateur pour émettre les jetons
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe invalide"));
        return buildAuthResponse(user);
    }

    /**
     * Renouvelle la session à l'aide d'un refresh token valide.
     * L'ancien jeton est révoqué (rotation) et un nouveau couple est émis.
     *
     * @param request le refresh token fourni par le client
     * @return un nouveau couple access/refresh + le profil utilisateur
     */
    @Transactional
    public AuthDtos.AuthResponse refresh(AuthDtos.RefreshRequest request) {
        // Recherche du jeton en base
        RefreshToken stored = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Session invalide"));
        // Jeton expiré ou révoqué → reconnexion impossible, oblige à se reloguer
        if (!stored.isValid()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Session expirée, veuillez vous reconnecter");
        }
        // Rotation : l'ancien jeton ne pourra plus être réutilisé (anti-rejeu)
        User user = stored.getUser();
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        return buildAuthResponse(user);
    }

    /**
     * Déconnexion : révoque le refresh token en base (invalidation de la session).
     *
     * @param refreshToken le refresh token à révoquer
     */
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    /**
     * Profil complet de l'utilisateur actuellement connecté.
     *
     * @return le DTO utilisateur avec ses identifiants de profil (clientId/technicianId)
     */
    public UserDtos.UserResponse me() {
        // Récupération de l'utilisateur connecté depuis le contexte de sécurité
        User user = SecurityUtils.currentUser();
        // Résolution des identifiants de profil métier (client ou technicien)
        Long clientId = clientRepository.findByUserId(user.getId()).map(Client::getId).orElse(null);
        Long technicianId = technicianRepository.findByUserId(user.getId()).map(Technician::getId).orElse(null);
        return UserMapper.toUserResponse(user, clientId, technicianId);
    }

    /**
     * Construit la réponse d'authentification : access token JWT + refresh token
     * persistant (durée configurée) + profil utilisateur.
     *
     * @param user l'utilisateur à authentifier
     * @return le triplet (accessToken, refreshToken, user)
     */
    private AuthDtos.AuthResponse buildAuthResponse(User user) {
        // Access token : JWT signé, courte durée
        String accessToken = jwtService.generateAccessToken(user);
        // Refresh token : UUID opaque persisté en base avec sa date d'expiration
        RefreshToken refreshToken = RefreshToken.builder()
                .token(jwtService.generateRefreshToken())
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(jwtProperties.refreshTokenValidityDays()))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        // L'utilisateur est prêt à être renvoyé au client (sans son mot de passe)
        return new AuthDtos.AuthResponse(
                accessToken,
                refreshToken.getToken(),
                new AuthDtos.AuthUser(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole())
        );
    }

    /**
     * Trouve une compétence par son nom, ou la crée si elle n'existe pas encore.
     *
     * @param name le nom de la compétence saisi au formulaire d'inscription
     * @return la compétence persistée (existante ou nouvelle)
     */
    private Skill findOrCreateSkill(String name) {
        String trimmed = name.trim();
        return skillRepository.findByNameIgnoreCase(trimmed)
                .orElseGet(() -> skillRepository.save(Skill.builder().name(trimmed).build()));
    }
}