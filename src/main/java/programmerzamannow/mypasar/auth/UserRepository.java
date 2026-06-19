package programmerzamannow.mypasar.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import programmerzamannow.mypasar.auth.entity.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    // Fungsi kustom untuk mencari user berdasarkan token di Redis/MySQL nanti
    Optional<User> findFirstByToken(String token);

    Optional<User> findFirstByUsername(String username);
}