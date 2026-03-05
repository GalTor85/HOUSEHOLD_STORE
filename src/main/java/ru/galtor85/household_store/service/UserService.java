package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.galtor85.household_store.dto.EditUserRequest;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.repository.UserRepository;

import java.time.LocalDate;

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

    public User edit(User user, EditUserRequest request) {
        if (Strings.isBlank(request.getFirstName()) && Strings.isBlank(request.getLastName())
                && Strings.isBlank(request.getSurname()) && Strings.isBlank(request.getEmail())
                && Strings.isBlank(request.getAddress()) && Strings.isBlank(request.getBirthDate())
                && Strings.isBlank(request.getMobileNumber()) && Strings.isBlank(request.getPassword())) {
            throw new IllegalArgumentException("Новые данные не предоставлены");
        }
        user.setFirstName(Strings.isBlank(request.getFirstName()) ? user.getFirstName() : request.getFirstName());
        user.setLastName(Strings.isBlank(request.getLastName()) ? user.getLastName() : request.getLastName());
        user.setSurname(Strings.isBlank(request.getSurname()) ? user.getSurname() : request.getSurname());
        user.setAddress(Strings.isBlank(request.getAddress()) ? user.getAddress() : request.getAddress());
        user.setEmail(Strings.isBlank(request.getEmail()) ? user.getEmail() : request.getEmail());
        user.setMobileNumber(Strings.isBlank(request.getMobileNumber()) ? user.getMobileNumber() : request.getMobileNumber());
        user.setBirthDate(request.getBirthDate() != null ? LocalDate.parse(request.getBirthDate()) : user.getBirthDate());
        if (Strings.isNotBlank(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        return userRepository.save(user);
    }

}