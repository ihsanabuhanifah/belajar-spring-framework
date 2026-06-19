package programmerzamannow.mypasar.shared.resolver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import programmerzamannow.mypasar.auth.UserRepository;
import programmerzamannow.mypasar.auth.entity.User;
import programmerzamannow.mypasar.shared.jwt.DecodeJwtService;

@Component
@Slf4j
public class UserArgumentResolver implements HandlerMethodArgumentResolver {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DecodeJwtService decodeJwtService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {

        return User.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        HttpServletRequest servletRequest = (HttpServletRequest) webRequest.getNativeRequest();
        String authorizationHeader = servletRequest.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized: Missing or Invalid Token Format");
        }

        String username = decodeJwtService.getCurrentName();

        log.info("username" + username);
        User user = userRepository.findFirstByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Not Found"));

        log.info("user resolver" + user);
        // if (user.getTokenExpireAt() < System.currentTimeMillis()) {
        // throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Not
        // Found");
        // }

        return user;
    }
}