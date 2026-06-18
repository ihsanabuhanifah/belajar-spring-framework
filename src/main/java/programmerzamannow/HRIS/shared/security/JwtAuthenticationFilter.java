package programmerzamannow.HRIS.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import programmerzamannow.HRIS.shared.jwt.JwtService;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/api/auth/register") || requestURI.startsWith("/api/auth/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. EKSTRAK DATA PERANGKAT DARI HEADER & REQUEST
        String userAgent = request.getHeader("User-Agent");
        String deviceId = request.getHeader("X-DEVICE-ID"); // Kita wajibkan header buatan sendiri
        String ipAddress = request.getHeader("X-Forwarded-For"); // Standar jika lewat proxy/gateway

        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr(); // Ambil IP langsung jika lokal
        }

        if (userAgent == null || userAgent.isBlank() ||
                deviceId == null || deviceId.isBlank() ||
                ipAddress == null || ipAddress.isBlank()) {

            log.warn("STRICT SECURITY DENIED: Missing device metadata. IP: [{}], DeviceID: [{}], UserAgent: [{}]",
                    ipAddress, deviceId, userAgent);

            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"errors\": \"Bad Request: Missing IP, Device ID, or User Agent in Header\"}");
            return; // Berhenti di sini, jangan lanjut ke filter/controller selanjutnya
        }

        log.info("Incoming request to URI: [{}] {}", request.getMethod(), request.getRequestURI());

        String authHeader = request.getHeader("Authorization");

        // Jika tidak ada header Authorization atau tidak diawali "Bearer ", abaikan dan
        // lanjut ke filter berikutnya
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7); // Potong teks "Bearer " untuk mengambil token murninya
        String username = jwtService.extractUsername(token);

        // Jika username ada dan user belum di-autentikasi di session saat ini
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Cek Session di Redis

            String redisKey = "session:" + username + ":" + deviceId;
            String tokenInRedis = redisTemplate.opsForValue().get(redisKey);

            if (jwtService.isTokenValid(token, username) && token.equals(tokenInRedis)) {

                log.info("AUTHENTICATION VERIFIED: User '{}' successfully accessed using matching device '{}'",
                        username, deviceId);

                // Buat objek autentikasi Spring Security
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username, null, new ArrayList<>());

                // Simpan ke dalam Context Security utama Spring
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                log.error("SECURITY ALERT: Invalid token for user '{}' from IP [{}]", username, ipAddress);
            }
        }

        filterChain.doFilter(request, response);
    }
}