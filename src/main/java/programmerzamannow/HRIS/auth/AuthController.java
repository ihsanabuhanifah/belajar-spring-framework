package programmerzamannow.HRIS.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import programmerzamannow.HRIS.auth.dto.LoginUserRequest;
import programmerzamannow.HRIS.auth.dto.RegisterUserRequest;
import programmerzamannow.HRIS.auth.dto.TokenResponse;
import programmerzamannow.HRIS.shared.response.WebResponse;

@RestController
// @RequestMapping()

public class AuthController {

    @Autowired
    private UserService userService;

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

}