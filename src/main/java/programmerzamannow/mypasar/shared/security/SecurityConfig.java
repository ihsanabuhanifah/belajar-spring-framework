package programmerzamannow.mypasar.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Bean
        public ObjectMapper objectMapper() {
                ObjectMapper mapper = new ObjectMapper();
                // Daftarkan modul bawaan agar tidak merusak Swagger dan handling Date/Time
                mapper.findAndRegisterModules();
                return mapper;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthFilter,
                        RateLimitFilter rateLimitFilter)
                        throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/api/auth/**", "/api/auth/register").permitAll()
                                                .requestMatchers("/api/v1/**")
                                                .authenticated() // Hanya ini yang dikunci wajib token
                                                .anyRequest().permitAll())
                                // --- TAMBAHKAN BLOK INI UNTUK MEMAKSA REsPONS MENJADI 401 UNAUTHORIZED ---
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                                                        response.setContentType("application/json");
                                                        response.getWriter().write("{\"errors\": \"Unauthorized\"}");
                                                }))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                // Ubah bagian penataan filter di SecurityConfig.java menjadi seperti ini:

                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                                .addFilterBefore(rateLimitFilter, JwtAuthenticationFilter.class);

                return http.build();
        }
}