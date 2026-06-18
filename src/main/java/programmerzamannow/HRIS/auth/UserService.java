package programmerzamannow.HRIS.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import programmerzamannow.HRIS.auth.dto.LoginUserRequest;
import programmerzamannow.HRIS.auth.dto.RegisterUserRequest;
import programmerzamannow.HRIS.auth.dto.TokenResponse;
import programmerzamannow.HRIS.auth.entity.User;
import programmerzamannow.HRIS.shared.jwt.BCrypt;
import programmerzamannow.HRIS.shared.jwt.JwtService;
import programmerzamannow.HRIS.shared.validation.ValidationService;

import java.time.Duration;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 1. LOGIKA REGISTER USER & EMPLOYEE BARU
     */
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterUserRequest request) {
        // Validasi input data menggunakan service lama Ama
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
        user = userRepository.saveAndFlush(user);

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
    public TokenResponse login(LoginUserRequest request) {
        // 1. Validasi input dari Postman
        validationService.validate(request);

        // 2. Cari user di database MySQL
        User user = userRepository.findById(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Username or password wrong"));

        // 3. Cek Password menggunakan BCrypt
        if (BCrypt.checkpw(request.getPassword(), user.getPassword())) {

            // 4. Buat token JWT sakti untuk user
            String token = jwtService.generateToken(user.getUsername(), user.getRole());

            // 5. Hitung waktu kedaluwarsa (Misal: 1 Jam = 3600 detik)
            long ttlInSeconds = 3600;
            long expiredAt = System.currentTimeMillis() + (ttlInSeconds * 1000);

            // Import yang harus dipastikan ada di paling atas file:
            // import java.time.Duration;

            try {
                // Nama loker penyimpanan di Redis
                String redisKey = "session:" + user.getUsername();

                // SOLUSI TERBARU: Menggunakan Duration.ofHours(1) atau Duration.ofSeconds(3600)
                redisTemplate.opsForValue().set(redisKey, token, Duration.ofSeconds(ttlInSeconds));

            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Redis is offline, cannot create session!");
            }
            // 7. Kembalikan token ke Postman
            return TokenResponse.builder()
                    .token(token)
                    .tokenExpiredAt(expiredAt)
                    .build();

        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Username or password wrong");
        }
    }

}