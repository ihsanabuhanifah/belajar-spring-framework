package programmerzamannow.mypasar.shared.jwt;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class DecodeJwtService {

    @Autowired
    private JwtService jwtService; // Suntikkan JwtService untuk membongkar klaim token

    @Autowired
    private HttpServletRequest request; // Untuk mengambil token dari Header HTTP secara otomatis

    // 1. Fungsi mempertahankan gaya lama (ambil username saja)
    public String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // 2. FUNGSI BARU: Langsung ambil nama dari token JWT
    public String getCurrentName() {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtService.extractUsername(token); // Memanggil fungsi extractName yang kita buat kemarin
        }
        return null;
    }
}