package ru.galtor85.household_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.galtor85.household_store.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
