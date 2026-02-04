package ru.galtor85.household_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;


import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByEmailContainingOrFirstNameContainingOrLastNameContaining(String email, String firstName, String lastName);
    List<User> findByRole(Role role);
}
