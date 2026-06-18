package programmerzamannow.restfull.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import programmerzamannow.restfull.entity.User;
import programmerzamannow.restfull.model.LoginUserRequest;
import programmerzamannow.restfull.model.TokenResponse;
import programmerzamannow.restfull.repository.UserRepository;
import programmerzamannow.restfull.security.BCrypt;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private JwtService jwtService;

    @Transactional
    public TokenResponse login(LoginUserRequest request) {
        validationService.validate(request);

        User user = userRepository.findById(request.getUsername())
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "username or password not found"));

        if (BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            user.setToken(UUID.randomUUID().toString());
            user.setTokenExpireAt(next30Days());

            userRepository.save(user);

            String token = jwtService.generateToken(user.getUsername(), user.getName());

            return TokenResponse.builder()
                    .token(token)
                    .expiredAt(user.getTokenExpireAt())
                    .build();

        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "username or password not found");
        }

    }

    @Transactional
    public void logout(User user) {
        user.setToken(null);
        user.setTokenExpireAt(null);
        userRepository.save(user);
    }

    private long next30Days() {
        return System.currentTimeMillis() + (1000 * 60 * 24 * 30);
    }

}
