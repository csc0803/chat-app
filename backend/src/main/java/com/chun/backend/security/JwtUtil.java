package com.chun.backend.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private Key getKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username) // payload 的 sub 欄位，存 username
                .setIssuedAt(new Date()) // 簽發時間
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs)) //過期時間
                .signWith(getKey(), SignatureAlgorithm.HS256) // 用 HS256 簽名
                .compact(); // 產生字串
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)   // 解析並驗證簽名
                .getBody()               // 取 payload
                .getSubject();           // 取 sub 欄位
    }

    public boolean validateToken(String token) {
        try{
            Jwts.parserBuilder().setSigningKey(getKey()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }


}
