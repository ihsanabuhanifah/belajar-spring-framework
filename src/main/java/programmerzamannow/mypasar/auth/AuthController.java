package programmerzamannow.mypasar.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import programmerzamannow.mypasar.auth.dto.LoginUserRequest;
import programmerzamannow.mypasar.auth.dto.ProfileResponse;
import programmerzamannow.mypasar.auth.dto.RegisterUserRequest;
import programmerzamannow.mypasar.auth.dto.TokenResponse;
import programmerzamannow.mypasar.shared.jwt.DecodeJwtService;
import programmerzamannow.mypasar.shared.response.WebResponse;

@Slf4j
@RestController
// @RequestMapping()

public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private DecodeJwtService decodeJwtService;

    /**
     * 1. ENDPOINT API: REGISTRASI USER & KARYAWAN BARU
     * URL: POST http://localhost:8080/api/users/register
     */
    @PostMapping(path = "/api/auth/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public WebResponse<String> register(@RequestBody RegisterUserRequest request) {
        // Kirim data ke UserService untuk divalidasi dan disimpan ke MySQL

        System.out.println("Req" + request.toString());
        userService.register(request);

        return WebResponse.<String>builder()
                .data("OK")
                .build();
    }

    /**
     * 2. ENDPOINT API: LOGIN USER (GENERATING REDIS SESSION)
     * URL: POST http://localhost:8080/api/auth/login
     */

    @PostMapping(path = "/api/auth/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public WebResponse<TokenResponse> register(@RequestBody LoginUserRequest request,
            @RequestHeader(name = "X-DEVICE-ID") String deviceId) {
        // Kirim data ke UserService untuk divalidasi dan disimpan ke MySQL

        TokenResponse tokenResponse = userService.login(request, deviceId);

        return WebResponse.<TokenResponse>builder().data(tokenResponse).build();
    }

    /**
     * 3. ENDPOINT API: MENAMPILKAN PROFIL USER YANG SEDANG LOGIN
     * URL: GET http://localhost:8080/api/auth/me
     */
    @GetMapping(path = "/api/auth/me", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public WebResponse<ProfileResponse> getProfile() {

        String username = decodeJwtService.getCurrentName();

        log.warn("usrname", username);

        ProfileResponse profileResponse = userService.getProfile(username);

        return WebResponse.<ProfileResponse>builder().data(profileResponse).build();
    }

}
