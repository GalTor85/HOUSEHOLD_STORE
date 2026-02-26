package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;  // Добавить

    public User register(User user) {

        if (userRepository.existsByEmail(user.getEmail())&&user.getEmail()!=null) {
            throw new RuntimeException(user.getEmail() + " already exists");
        }
        if (userRepository.existsByMobileNumber(user.getMobileNumber())&&user.getMobileNumber()!=null) {
            throw new RuntimeException(user.getMobileNumber() + " already exists");
        }
        // Хешируем пароль
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public User login(String password, String value) {
        User user = userRepository.findByEmailOrMobileNumber(value, value)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        // Проверяем через passwordEncoder
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated");
        }

        return user;
    }

}