package ru.galtor85.household_store.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.repository.UserRepository;
import ru.galtor85.household_store.service.MessageService;

import java.time.LocalDate;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    @Lazy
    private final PasswordEncoder passwordEncoder;
    private final MessageService messageService;

    @Override
    public void run(String... args) {
        Locale locale = Locale.getDefault();

        // Создаем администратора по умолчанию если его нет
        if (userRepository.findByEmail("admin@household.store").isEmpty()) {
            User admin = User.builder()
                    .email("admin@household.store")
                    .password(passwordEncoder.encode("Admin123!"))
                    .firstName(messageService.get("admin-initializer.admin.default.firstname"))
                    .lastName(messageService.get("admin-initializer.admin.default.lastname"))
                    .birthDate(LocalDate.now())
                    .role(Role.ADMIN)
                    .creator(messageService.get("admin-initializer.system"))
                    .active(true)
                    .build();

            userRepository.save(admin);
            log.info(messageService.get("admin-initializer.log.admin.created",
                    "admin@household.store", "Admin123!"));
        }

        // Создаем менеджера по умолчанию
        if (userRepository.findByEmail("manager@household.store").isEmpty()) {
            User manager = User.builder()
                    .email("manager@household.store")
                    .password(passwordEncoder.encode("Manager123!"))
                    .firstName(messageService.get("admin-initializer.manager.default.firstname"))
                    .lastName(messageService.get("admin-initializer.manager.default.lastname"))
                    .birthDate(LocalDate.now())
                    .role(Role.MANAGER)
                    .creator(messageService.get("admin-initializer.system"))
                    .active(true)
                    .build();

            userRepository.save(manager);
            log.info(messageService.get("admin-initializer.log.manager.created",
                    "manager@household.store", "Manager123!"));
        }
    }
}