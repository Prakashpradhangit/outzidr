package com.outzdir.in.outzdir.Security;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.outzdir.in.outzdir.Entity.Users;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class AuthUtilTest {

    private AuthUtil authUtil;
    private final String testSecretKey = "abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890";
    private Users testUser;

    @BeforeEach
    void setUp() {
        authUtil = new AuthUtil();
        ReflectionTestUtils.setField(authUtil, "jwtSerectKey", testSecretKey);

        testUser = new Users();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
    }


    @Test
    void testGenerateAccessToken() {
        String token = authUtil.generateAccessToken(testUser);
        assertNotNull(token);
        assertTrue(authUtil.validateToken(token));
        assertEquals("access", authUtil.getTokenType(token));
        assertEquals("test@example.com", authUtil.getEmailFromToken(token));
    }

    

    @Test
    void testGenerateRefreshToken() {
        String token = authUtil.generateRefreshToken(testUser);
        assertNotNull(token);
        assertTrue(authUtil.validateToken(token));
        assertEquals("refresh", authUtil.getTokenType(token));
        assertEquals("test@example.com", authUtil.getEmailFromToken(token));
    }

    @Test
    void testValidateToken_InvalidSignature() {
        String token = authUtil.generateAccessToken(testUser);
        String invalidToken = token + "modified";
        assertFalse(authUtil.validateToken(invalidToken));
    }

    @Test
    void testValidateToken_ExpiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(testSecretKey.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject(testUser.getEmail())
                .claim("type", "access")
                .issuedAt(new Date(System.currentTimeMillis() - 10000))
                .expiration(new Date(System.currentTimeMillis() - 5000))
                .signWith(key)
                .compact();

        assertFalse(authUtil.validateToken(expiredToken));
    }

    @Test
    void testGetTokenType_InvalidToken() {
        assertNull(authUtil.getTokenType("invalid.token.here"));
    }
}
