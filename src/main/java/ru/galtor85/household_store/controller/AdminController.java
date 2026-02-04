package ru.galtor85.household_store.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.galtor85.household_store.service.AuthMiddlewareService;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthMiddlewareService authMiddlewareService;

    // Другие админские маршруты, не связанные с пользователями
    @GetMapping("/settings")
    public String settings(HttpSession session) {
        authMiddlewareService.getAdminUser(session);
        return "admin/settings";
    }

    @GetMapping("/logs")
    public String logs(HttpSession session) {
        authMiddlewareService.getAdminUser(session);
        return "admin/logs";
    }
}
