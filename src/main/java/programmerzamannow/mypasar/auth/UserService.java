package programmerzamannow.mypasar.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import programmerzamannow.mypasar.auth.dto.LoginUserRequest;
import programmerzamannow.mypasar.auth.dto.ProfileResponse;
import programmerzamannow.mypasar.auth.dto.RegisterUserRequest;
import programmerzamannow.mypasar.auth.dto.TokenResponse;
import programmerzamannow.mypasar.auth.entity.User;
import programmerzamannow.mypasar.employee.EmployeeRepository;
import programmerzamannow.mypasar.employee.entity.Employee;
import programmerzamannow.mypasar.shared.jwt.BCrypt;
import programmerzamannow.mypasar.shared.jwt.JwtService;
import programmerzamannow.mypasar.shared.validation.ValidationService;

import java.time.Duration;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterUserRequest request) {
        // Validasi input data menggunakan service lama Ama
        log.info("Attempting to register new user with username: '{}'", request.getUsername());
        validationService.validate(request);

        // Cek apakah username sudah terpakai di MySQL
        if (userRepository.existsById(request.getUsername())) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username already exists");

        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));
        user.setRole(request.getRole());

        // Simpan USER dan paksa MySQL untuk langsung mencatatnya di memori transaksi
        user = userRepository.save(user);
        log.info("User '{}' successfully registered to database", request.getUsername()); // <-- Log sukses

        // // 3. BUAT DAN SIMPAN EMPLOYEE
        // Employee employee = new Employee();
        // employee.setId(UUID.randomUUID().toString());
        // employee.setFirstName(request.getFirstName());
        // employee.setLastName(request.getLastName());
        // employee.setEmail(request.getEmail());
        // employee.setJobTitle(request.getJobTitle());

        // // TEMPELKAN OBJEK USER YANG SUDAH VALID DI ATAS KE SINI
        // employee.setUser(user);

        // // Simpan EMPLOYEE
        // employeeRepository.save(employee);
    }

    @Transactional
    public TokenResponse login(LoginUserRequest request, String deviceId) {
        log.info("User '{}' is trying to login", request.getUsername());

        validationService.validate(request);
        User user = userRepository.findById(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Username or password wrong"));
        if (BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            String token = jwtService.generateToken(user.getUsername(), user.getRole());
            long ttlInSeconds = 3600;
            long expiredAt = System.currentTimeMillis() + (ttlInSeconds * 1000);

            try {
                // Nama loker penyimpanan di Redis
                String redisKey = "session:" + user.getUsername() + ":" + deviceId;

                // SOLUSI TERBARU: Menggunakan Duration.ofHours(1) atau Duration.ofSeconds(3600)
                redisTemplate.opsForValue().set(redisKey, token, Duration.ofSeconds(ttlInSeconds));

                log.info("Session linked and saved in Redis for user '{}' on device '{}'", user.getUsername(),
                        deviceId);
            } catch (Exception e) {
                log.error("Failed to save session to Redis for user '{}'. Error: {}", user.getUsername(),
                        e.getMessage()); // <-- Log eror sistem
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Redis is offline, cannot create session!");
            }
            return TokenResponse.builder()
                    .token(token)
                    .tokenExpiredAt(expiredAt)
                    .build();

        } else {
            log.warn("Login failed: Wrong password for username '{}'", request.getUsername());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Username or password wrong");
        }
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String username) {
        log.info("FETCH PROFILE - User '{}' is requesting profile details via /api/auth/me", username);

        String cacheKey = "cache:profile:" + username;

        try {
            String cachedProfile = redisTemplate.opsForValue().get(cacheKey);
            if (cachedProfile != null) {
                log.info("FETCH PROFILE HIT - Returning profile from Redis for user '{}'", username);
                return objectMapper.readValue(cachedProfile, ProfileResponse.class);
            }
        } catch (Exception e) {
            log.error("CACHE ERROR FOR USER '{}'. Error: {}", username, e.getMessage());

        }
        User user = userRepository.findFirstByUsername(username)
                .orElseThrow(() -> {
                    log.error("FETCH PROFILE FAILED - Security anomaly: Username '{}' not found!", username);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
                });

        log.info("FETCH PROFILE - User '{}' validated. Fetching data from employees table...", username);
        Employee employee = employeeRepository.findByUsername(user.getUsername()).orElse(null);

        ProfileResponse.ProfileResponseBuilder builder = ProfileResponse.builder()
                .username(user.getUsername())
                .role(user.getRole());

        if (employee == null) {
            log.warn("FETCH PROFILE WARN - User '{}' has no linked Employee biography record", username);
        } else {

            builder.firstName(employee.getFirstName())
                    .lastName(employee.getLastName())
                    .email(employee.getEmail())
                    .phone(employee.getPhone())
                    .jobTitle(employee.getJobTitle())
                    .joinedAt(employee.getJoinedAt());

            log.info("FETCH PROFILE SUCCESS - Detailed profile for '{}' (Job: '{}') compiled successfully",
                    username, employee.getJobTitle());
        }

        return builder.build();
    }

}