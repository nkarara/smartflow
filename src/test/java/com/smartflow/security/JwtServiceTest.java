package com.smartflow.security;

import com.smartflow.entity.Role;
import com.smartflow.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Génération et validation des access tokens JWT.
 */
class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            new JwtProperties("test-secret-key-for-unit-tests-min-32-bytes-ok", 15, 7));

    private User buildUser() {
        User user = new User();
        user.setId(42L);
        user.setEmail("jean@smartflow.fr");
        user.setRole(Role.CLIENT);
        return user;
    }

    @Test
    void generatesAndParsesAccessToken() {
        User user = buildUser();
        String token = jwtService.generateAccessToken(user);

        assertTrue(jwtService.isAccessTokenValid(token));
        assertEquals("jean@smartflow.fr", jwtService.extractEmail(token));
        assertEquals(42L, jwtService.extractUserId(token));
    }

    @Test
    void rejectsInvalidOrExpiredTokens() {
        assertFalse(jwtService.isAccessTokenValid("tampered.token.value"));
        assertFalse(jwtService.isAccessTokenValid(null));
        assertFalse(jwtService.isAccessTokenValid(""));
    }
}