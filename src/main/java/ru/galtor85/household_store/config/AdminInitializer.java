package ru.galtor85.household_store.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@Component
@Slf4j
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    @Lazy
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Создаем администратора по умолчанию если его нет
        if (userRepository.findByEmail("admin@household.store").isEmpty()) {
            User admin = User.builder()
                    .email("admin@household.store")
                    .password(passwordEncoder.encode("Admin123!"))
                    .firstName("Администратор")
                    .lastName("Системы")
                    .birthDate(LocalDate.now())
                    .role(Role.ADMIN)
                    .active(true)
                    .build();

            userRepository.save(admin);
            log.info("✅ Создан администратор по умолчанию: admin@household.store / Admin123!");
        }

        // Создаем менеджера по умолчанию
        if (userRepository.findByEmail("manager@household.store").isEmpty()) {
            User manager = User.builder()
                    .email("manager@household.store")
                    .password(passwordEncoder.encode("Manager123!"))
                    .firstName("Менеджер")
                    .lastName("Системы")
                    .birthDate(LocalDate.now())
                    .role(Role.MANAGER)
                    .active(true)
                    .build();

            userRepository.save(manager);
            log.info("✅ Создан менеджер по умолчанию: manager@household.store / Manager123!");
        }
    }
}