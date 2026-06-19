package programmerzamannow.mypasar.shared.exception;

import lombok.extern.slf4j.Slf4j;
import programmerzamannow.mypasar.shared.response.WebResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
@Slf4j
public class ErrorController {

    // 1. Menangkap semua eror ResponseStatusException (seperti 401, 400, 404 yang
    // kita buat manual)

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<WebResponse<String>> constraintViolationException(ConstraintViolationException exception) {

        // Mengambil pesan eror pertama yang melanggar aturan
        String errorMessage = exception.getConstraintViolations().iterator().next().getMessage();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST) // 🌟 Ubah status jadi 400 Bad Request
                .body(WebResponse.<String>builder()
                        .errors(errorMessage) // Masukkan pesan erornya ke field 'errors'
                        .build());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatusException(ResponseStatusException exception) {
        // Catat erornya ke dalam log dengan level WARN
        log.warn("API Error occurred: Status Code [{}], Reason: {}", exception.getStatusCode(), exception.getReason());

        return ResponseEntity.status(exception.getStatusCode())
                .body("{\"errors\": \"" + exception.getReason() + "\"}");
    }

    // 2. Menangkap eror tidak terduga (seperti NullPointerException, MySQL down,
    // dll)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception exception) {
        // Catat seluruh stack trace erornya dengan level ERROR agar kita bisa lacak
        // baris kodenya
        log.error("CRITICAL SYSTEM ERROR: ", exception);

        return ResponseEntity.status(500)
                .body("{\"errors\": \"Internal server error, please check system log.\"}");
    }
}