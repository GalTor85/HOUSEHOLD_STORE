package ru.galtor85.household_store.controller;


import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.galtor85.household_store.dto.ChangeRoleRequest;
import ru.galtor85.household_store.dto.CreateUserRequest;
import ru.galtor85.household_store.dto.UserResponse;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.service.UserManagementService;


import org.springframework.security.access.AccessDeniedException;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserManagementService userManagementService;

    // ========== MIDDLEWARE ДЛЯ ПРОВЕРКИ ПРАВ ==========

    private User getAdminUser(HttpSession session) throws AccessDeniedException {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("Требуется авторизация");
        }
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.MANAGER) {
            throw new AccessDeniedException("Доступ запрещен. Требуется роль ADMIN или MANAGER");
        }
        return user;
    }

    // ========== ВЕБ-ИНТЕРФЕЙС ==========

    @GetMapping("/users")
    public String usersPage(HttpSession session, Model model,
                            @RequestParam(required = false) String search) throws AccessDeniedException {
        User adminUser = getAdminUser(session);

        List<User> users;
        if (search != null && !search.trim().isEmpty()) {
            users = userManagementService.searchUsers(search.trim());
        } else {
            users = userManagementService.getAllUsers(adminUser);
        }

        List<UserResponse> userResponses = users.stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());

        model.addAttribute("users", userResponses);
        model.addAttribute("search", search);
        model.addAttribute("currentUser", adminUser);
        model.addAttribute("availableRoles", Role.values());
        model.addAttribute("changeRoleRequest", new ChangeRoleRequest());
        model.addAttribute("createUserRequest", new CreateUserRequest());

        return "admin/users";
    }

    @PostMapping("/users/change-role")
    public String changeRole(@Valid @ModelAttribute ChangeRoleRequest request,
                             BindingResult bindingResult,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        try {
            User adminUser = getAdminUser(session);

            if (bindingResult.hasErrors()) {
                redirectAttributes.addFlashAttribute("error", "Ошибка валидации");
                return "redirect:/admin/users";
            }

            User updatedUser = userManagementService.changeUserRole(
                    adminUser, request.getUserId(), request.getNewRole());

            redirectAttributes.addFlashAttribute("success",
                    "Роль пользователя " + updatedUser.getEmail() + " изменена на " + request.getNewRole());

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/users/toggle-active/{userId}")
    public String toggleActive(@PathVariable Long userId,
                               @RequestParam boolean active,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        try {
            User adminUser = getAdminUser(session);
            User updatedUser = userManagementService.toggleUserActive(adminUser, userId, active);

            String message = active ?
                    "Пользователь " + updatedUser.getEmail() + " активирован" :
                    "Пользователь " + updatedUser.getEmail() + " деактивирован";

            redirectAttributes.addFlashAttribute("success", message);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/users/create")
    public String createUser(@Valid @ModelAttribute CreateUserRequest request,
                             BindingResult bindingResult,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        try {
            User adminUser = getAdminUser(session);

            if (bindingResult.hasErrors()) {
                redirectAttributes.addFlashAttribute("error", "Ошибка валидации");
                return "redirect:/admin/users";
            }

            User newUser = User.builder()
                    .email(request.getEmail())
                    .password(request.getPassword())
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .surname(request.getSurname())
                    .role(request.getRole())
                    .active(request.isActive())
                    .build();

            User createdUser = userManagementService.createUserWithRole(adminUser, newUser, request.getRole());

            redirectAttributes.addFlashAttribute("success",
                    "Пользователь " + createdUser.getEmail() + " создан с ролью " + request.getRole());

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/users";
    }
}
