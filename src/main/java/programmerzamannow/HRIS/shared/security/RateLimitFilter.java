package programmerzamannow.HRIS.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Duration;

@Component
@Slf4j
@Order(1) // 🌟 KUNCI UTAMA: Biar filter ini jalan PERTAMA KALI sebelum filter lainnya
public class RateLimitFilter extends OncePerRequestFilter {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final int MAX_REQUESTS_PER_MINUTE = 5;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Ambil IP Address
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }

        // 2. Ambil Path dan Method HTTP
        String path = request.getRequestURI();
        String method = request.getMethod();

        // 3. Rakit Kunci Spesifik per Endpoint
        String rateKey = "rate:limit:" + ipAddress + ":" + method + ":" + path;

        // 4. Hitung Request di Redis
        Long currentRequests = redisTemplate.opsForValue().increment(rateKey);

        if (currentRequests != null && currentRequests == 1) {
            redisTemplate.expire(rateKey, Duration.ofMinutes(1));
        }

        // 5. Cek Batasan
        if (currentRequests != null && currentRequests > MAX_REQUESTS_PER_MINUTE) {
            log.warn("RATE LIMIT EXCEEDED! IP [{}] blocked on Endpoint [{}] {} -> {}/{}",
                    ipAddress, method, path, currentRequests, MAX_REQUESTS_PER_MINUTE);

            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter()
                    .write("{\"errors\": \"Too Many Requests on this specific feature. Please wait a minute.\"}");
            return; // STOP di sini, jangan lanjut ke filter JWT atau Controller!
        }

        // Lolos! Silakan lanjut ke satpam berikutnya (JwtAuthenticationFilter)
        filterChain.doFilter(request, response);
    }
}