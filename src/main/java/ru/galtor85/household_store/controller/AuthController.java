package ru.galtor85.household_store.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.galtor85.household_store.dto.LoginForm;
import ru.galtor85.household_store.dto.RegisterForm;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.service.UserService;

@Slf4j
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    // ========== LOGIN ==========

    @GetMapping("/login")
    public String showLoginPage(Model model) {
        if (!model.containsAttribute("loginForm")) {
            model.addAttribute("loginForm", new LoginForm());
        }
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("loginForm") LoginForm loginForm,
                        BindingResult bindingResult,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.loginForm", bindingResult);
            redirectAttributes.addFlashAttribute("loginForm", loginForm);
            return "redirect:/auth/login";
        }

        try {
            User user = userService.login(loginForm.getEmail(), loginForm.getPassword());
            session.setAttribute("user", user);
            log.info("User logged in: {}", user.getEmail());

            // Редирект в зависимости от роли
            return switch (user.getRole()) {
                case ADMIN -> "redirect:/admin/users";
                case USER -> "redirect:/user/dashboard";
                default -> "redirect:/auth/login";
            };

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("loginForm", loginForm);
            return "redirect:/auth/login";
        }
    }

    // ========== REGISTER ==========

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterForm());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm registerForm,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.registerForm", bindingResult);
            redirectAttributes.addFlashAttribute("registerForm", registerForm);
            return "redirect:/auth/register";
        }

        try {
            // Преобразуем DTO в Entity
            User user = User.builder()
                    .email(registerForm.getEmail())
                    .password(registerForm.getPassword()) // Service захеширует
                    .firstName(registerForm.getFirstName())
                    .lastName(registerForm.getLastName())
                    .surname(registerForm.getSurname())
                    .birthDate(registerForm.getBirthDate())
                    .role(Role.USER)
                    .creator(registerForm.getFirstName()+" "+registerForm.getLastName()+" email:"+registerForm.getEmail())
                    .active(true)
                    .build();

            userService.register(user);
            log.info("User registered: {}", user.getEmail());

            redirectAttributes.addFlashAttribute("success",
                    "Регистрация успешна! Теперь вы можете войти в систему.");
            return "redirect:/auth/login";

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("registerForm", registerForm);
            return "redirect:/auth/register";
        }
    }

    // ========== LOGOUT ==========

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        if (session != null) {
            User user = (User) session.getAttribute("user");
            if (user != null) {
                log.info("User logged out: {}", user.getEmail());
            }
            session.invalidate();
        }
        return "redirect:/auth/login";
    }

    // ========== REST API (оставляем для мобильных приложений) ==========

    @RestController
    @RequestMapping("/api/auth")
    @RequiredArgsConstructor
    public static class AuthApiController {
        private final UserService userService;

        @PostMapping("/register")
        public ResponseEntity<User> register(@RequestBody User user) {
            return ResponseEntity.ok(userService.register(user));
        }

        @PostMapping("/login")
        public ResponseEntity<User> login(@RequestBody LoginRequest request) {
            return ResponseEntity.ok(userService.login(request.email(), request.password()));
        }

        public record LoginRequest(String email, String password) {}
    }
}