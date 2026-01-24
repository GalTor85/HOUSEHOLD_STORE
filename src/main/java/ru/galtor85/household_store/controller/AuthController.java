package ru.galtor85.household_store.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.galtor85.household_store.model.User;
import ru.galtor85.household_store.service.UserService;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) {
        return ResponseEntity.ok(userService.register(user));
    }

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody LoginRequest request) {
        // Используем методы record без префикса get
        return ResponseEntity.ok(userService.login(request.email(), request.password()));
    }

    // Внутренний класс для запроса - record автоматически создает методы
    public record LoginRequest(String email, String password) {}
}
