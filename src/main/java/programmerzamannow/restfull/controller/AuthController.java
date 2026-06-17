package programmerzamannow.restfull.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import programmerzamannow.restfull.entity.User;
import programmerzamannow.restfull.model.LoginUserRequest;
import programmerzamannow.restfull.model.TokenResponse;
import programmerzamannow.restfull.model.UserResponse;
import programmerzamannow.restfull.model.WebResponse;
import programmerzamannow.restfull.service.AuthService;
import programmerzamannow.restfull.service.DecodeJwtService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private DecodeJwtService decodeJwtService;

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<TokenResponse> login(@RequestBody LoginUserRequest request) {
        TokenResponse tokenResponse = authService.login(request);
        return WebResponse.<TokenResponse>builder().data(tokenResponse).build();
    }

    @PostMapping(path = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)

    public WebResponse<String> logout(User user) {
        authService.logout(user);
        return WebResponse.<String>builder().data("OK").build();
    }

    @GetMapping(path = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<UserResponse> getMe() {

        String username = decodeJwtService.getCurrentUsername();
        String name = decodeJwtService.getCurrentName();

        UserResponse userResponse = UserResponse.builder()
                .username(username)
                .name(name)
                .build();

        // 3. Mengembalikan respons data "OK" sesuai keinginan Ama
        return WebResponse.<UserResponse>builder()
                .data(userResponse)
                .build();
    }

}
