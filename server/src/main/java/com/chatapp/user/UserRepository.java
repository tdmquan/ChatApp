package com.chatapp.user;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
            SELECT u FROM User u
            WHERE u.id <> :excludeId
              AND (LOWER(u.email) LIKE :pattern ESCAPE '\\'
                OR LOWER(u.firstName) LIKE :pattern ESCAPE '\\'
                OR LOWER(u.lastName) LIKE :pattern ESCAPE '\\'
                OR LOWER(CONCAT(COALESCE(u.firstName, ''), ' ', COALESCE(u.lastName, ''))) LIKE :pattern ESCAPE '\\')
            ORDER BY u.firstName, u.lastName, u.email
            """)
    List<User> search(Long excludeId, String pattern, Pageable pageable);

    List<User> findByIdNotOrderByFirstNameAscLastNameAscEmailAsc(Long excludeId);
}
