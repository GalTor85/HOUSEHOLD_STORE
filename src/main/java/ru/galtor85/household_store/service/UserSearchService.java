package ru.galtor85.household_store.service;

import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.repository.UserRepository;


import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserSearchService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<User> getAllUsers(String sort) {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, sort != null ? sort.trim() : "id"));
    }

    @Transactional(readOnly = true)
    public List<User> searchUsersByCriteria(String mobileNumber, String email, String firstName, String lastName, String sort) {
        return userRepository.findByEmailContainingOrMobileNumberContainingOrFirstNameContainingOrLastNameContaining(
                email != null ? email.trim() : null,
                mobileNumber != null ? mobileNumber.trim() : null,
                firstName != null ? firstName.trim() : null,
                lastName != null ? lastName.trim() : null,
                Sort.by(Sort.Direction.ASC, sort != null ? sort.trim() : "id"));
    }



    @Transactional(readOnly = true)
    public Optional<User>  searchUsersByEmailOrMobileNumber(String identify) {
        return userRepository.findByEmailOrMobileNumber(
                identify, identify);
    }

    @Transactional(readOnly = true)
    public List<User> findUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }

    @Transactional(readOnly = true)
    public boolean userExistsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email){
        return userRepository.findByEmail(email);
    }

    /**
     * Возвращает пользователя с Id
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

}