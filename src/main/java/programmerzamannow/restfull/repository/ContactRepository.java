package programmerzamannow.restfull.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import programmerzamannow.restfull.entity.Contact;
import programmerzamannow.restfull.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface ContactRepository extends JpaRepository<Contact, String>, JpaSpecificationExecutor<Contact> {

    Optional<Contact> findFirstByUserAndId(User user, String id);

    @Query(value = "SELECT * FROM contacts WHERE user_username = :username " +
            "AND (:name IS NULL OR first_name LIKE %:name% OR last_name LIKE %:name%) " +
            "AND (:email IS NULL OR email LIKE %:email%) " +
            "AND (:phone IS NULL OR phone LIKE %:phone%)", countQuery = "SELECT count(*) FROM contacts WHERE user_username = :username "
                    +
                    "AND (:name IS NULL OR first_name LIKE %:name% OR last_name LIKE %:name%) " +
                    "AND (:email IS NULL OR email LIKE %:email%) " +
                    "AND (:phone IS NULL OR phone LIKE %:phone%)", nativeQuery = true)
    Page<Contact> searchNative(
            @Param("username") String username,
            @Param("name") String name,
            @Param("email") String email,
            @Param("phone") String phone,
            Pageable pageable);
}
