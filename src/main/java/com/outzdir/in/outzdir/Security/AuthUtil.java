package com.outzdir.in.outzdir.Security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.outzdir.in.outzdir.Entity.Users;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class AuthUtil {

    @Value("${jwr.secrectkey}")
    private String jwtSerectKey;

    private SecretKey getSecrectKey(){
        return Keys.hmacShaKeyFor(jwtSerectKey.getBytes(StandardCharsets.UTF_8));
    }

    //Generate Access Token
    public String generateAccessToken(Users users){
        return Jwts.builder()
                    .subject(users.getEmail())
                    .claim("type", "access")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 15 * 60 * 1000))
                    .signWith(getSecrectKey())
                    .compact();
    }

    //Generate Refrsh Token
    public String generateRefreshToken(Users users){
        return Jwts.builder()
                    .subject(users.getEmail())
                    .claim("type", "refresh")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 20))
                    .signWith(getSecrectKey())
                    .compact();
    }

    public String getTokenType(String token) {
        try {
            return Jwts.parser()
                        .verifyWith(getSecrectKey())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
                        .get("type", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    //Extract email from the token
    public String getEmailFromToken(String token){
        return Jwts.parser()
                    .verifyWith(getSecrectKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
    }

    //Validate the token
    public boolean validateToken(String token){
        try {
            Jwts.parser()
                .verifyWith(getSecrectKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
