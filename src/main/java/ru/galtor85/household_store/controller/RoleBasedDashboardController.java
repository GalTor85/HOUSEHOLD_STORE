package ru.galtor85.household_store.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.service.AuthMiddlewareService;

import jakarta.servlet.http.HttpSession;

@Slf4j
@Controller
@RequiredArgsConstructor
public class RoleBasedDashboardController {

    private final AuthMiddlewareService authMiddlewareService;

    @GetMapping("/admin")
    public String adminDashboard(Model model, HttpSession session) {
        User user = authMiddlewareService.getAdminUser(session);
        model.addAttribute("user", user);
        model.addAttribute("title", "Админ-панель");
        return "admin/dashboard";
    }

    @GetMapping("/user/dashboard")
    public String userDashboard(Model model, HttpSession session) {
        User user = authMiddlewareService.getUserWithRequiredRole(session, Role.USER);
        model.addAttribute("user", user);
        model.addAttribute("title", "Личный кабинет");
        return "user/dashboard";
    }

    // Опционально: общая дашборд для всех аутентифицированных пользователей
    @GetMapping("/dashboard")
    public String commonDashboard(Model model, HttpSession session) {
        User user = authMiddlewareService.getAuthenticatedUser(session);
        model.addAttribute("user", user);
        model.addAttribute("title", "Панель управления");

        // Перенаправляем в зависимости от роли
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.MANAGER) {
            return "redirect:/admin";
        } else if (user.getRole() == Role.USER) {
            return "redirect:/user/dashboard";
        }

        return "common/dashboard";
    }
}