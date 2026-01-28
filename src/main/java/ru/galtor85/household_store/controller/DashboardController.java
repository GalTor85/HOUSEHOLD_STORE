package ru.galtor85.household_store.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;

import jakarta.servlet.http.HttpSession;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DashboardController {

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || user.getRole() != Role.ADMIN) {
            return "redirect:/auth/login";        }
        model.addAttribute("user", user);
        model.addAttribute("title", "Админ-панель");
        return "admin/dashboard"; // шаблон templates/admin/dashboard.html
    }

    @GetMapping("/user/dashboard")
    public String userDashboard(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || user.getRole() != Role.USER) {
            return "redirect:/auth/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("title", "Личный кабинет");
        return "user/dashboard"; // шаблон templates/user/dashboard.html
    }
}