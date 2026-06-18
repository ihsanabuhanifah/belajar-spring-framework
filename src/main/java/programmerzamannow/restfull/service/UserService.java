package programmerzamannow.restfull.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import programmerzamannow.restfull.entity.User;
import programmerzamannow.restfull.model.RegisterUserRequest;
import programmerzamannow.restfull.model.UpdateUserRequest;
import programmerzamannow.restfull.model.UserResponse;
import programmerzamannow.restfull.repository.UserRepository;
import programmerzamannow.restfull.security.BCrypt;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ValidationService validationService;

    @Transactional
    public void register(RegisterUserRequest request) {

        validationService.validate(request);

        if (userRepository.existsById(request.getUsername())) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username already exists");

        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));
        user.setName(request.getName());

        userRepository.save(user);

    }

    public UserResponse get(User user) {
        return UserResponse.builder()
                .username(user.getUsername())
                .name(user.getName())
                .token(user.getToken())
                .build();
    }

    @Transactional
    public UserResponse update(User user, UpdateUserRequest request) {
        validationService.validate(request);

        if (request.getPassword() != null) {
            user.setPassword(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));
        }

        if (request.getName() != null) {
            user.setName(request.getName());
        }

        System.out.println("name " + request);

        userRepository.save(user);

        return UserResponse.builder()
                .username(user.getUsername())
                .name(user.getName())
                .token(user.getToken())
                .build();
    }

}
