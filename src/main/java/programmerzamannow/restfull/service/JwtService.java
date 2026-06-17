package programmerzamannow.restfull.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    // Kunci rahasia minimal 32 karakter
    // Tambahkan titik dua (:) diikuti nilai cadangan jika .env tidak terbaca
    @Value("${JWT_SECRET:KunciRahasiaCadanganMinimalHarusTigaPuluhDuaKarakterYaAma}")
    private String secretKeyString;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKeyString.getBytes());
    }

    // 1. Membuat Token JWT dengan gaya Baru (JJWT 0.12.x)
    public String generateToken(String username, String name) {
        return Jwts.builder()
                .subject(username) // Tetap jadikan username sebagai identitas utama (Subject)
                .claim("name", name) //
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // 24 Jam
                .signWith(getSigningKey()) // Algoritma otomatis dideteksi dari SecretKey
                .compact();
    }

    // 2. Mengekstrak Claims & Username dengan gaya Baru (JJWT 0.12.x)
    public String extractUsername(String token) {
        Claims claims = Jwts.parser() // Pakai parser(), bukan parserBuilder()
                .verifyWith(getSigningKey()) // Pakai verifyWith(), bukan setSigningKey()
                .build()
                .parseSignedClaims(token) // Pakai parseSignedClaims(), bukan parseClaimsJws()
                .getPayload(); // Pakai getPayload(), bukan getBody()

        return claims.getSubject();
    }

    // 3. Validasi Token
    public boolean isTokenValid(String token, String username) {
        String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username));
    }
}