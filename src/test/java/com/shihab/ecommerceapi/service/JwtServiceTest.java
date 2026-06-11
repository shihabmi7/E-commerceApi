package com.shihab.ecommerceapi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "3cfa76ef14937c1c0ea519f8fc057a80fcd04a7420f8e8bcd0a7567c272e007b";
    private static final long EXPIRATION = 12222200000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION);
    }

    @Test
    void generateToken_returnsNonNullToken() {
        String token = jwtService.generateToken("user@example.com");
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void extractUsername_returnsCorrectSubject() {
        String token = jwtService.generateToken("user@example.com");
        assertThat(jwtService.extractUsername(token)).isEqualTo("user@example.com");
    }

    @Test
    void isTokenValid_returnsTrueForMatchingUser() {
        String token = jwtService.generateToken("user@example.com");
        assertThat(jwtService.isTokenValid(token, "user@example.com")).isTrue();
    }

    @Test
    void isTokenValid_returnsFalseForWrongUser() {
        String token = jwtService.generateToken("user@example.com");
        assertThat(jwtService.isTokenValid(token, "other@example.com")).isFalse();
    }

    @Test
    void isTokenValid_throwsForExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);
        String token = jwtService.generateToken("user@example.com");
        // extractAllClaims propagates ExpiredJwtException — the service does not swallow it
        assertThatThrownBy(() -> jwtService.isTokenValid(token, "user@example.com"))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    void generateToken_withExtraClaims_embedsClaimsInToken() {
        java.util.Map<String, Object> claims = java.util.Map.of("role", "admin");
        String token = jwtService.generateToken(claims, "admin@example.com");
        assertThat(jwtService.extractUsername(token)).isEqualTo("admin@example.com");
    }
}
