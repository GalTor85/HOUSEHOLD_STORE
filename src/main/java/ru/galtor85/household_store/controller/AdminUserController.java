package ru.galtor85.household_store.controller;

import jakarta.persistence.EntityNotFoundException;
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
import ru.galtor85.household_store.service.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {


    private final AuthMiddlewareService authMiddlewareService;
    private final UserSearchService userSearchService;
    private final UserRoleService userRoleService;
    private final UserStatusService userStatusService;
    private final AdminUserCreationService adminUserCreationService;
    private final UserDeletedService userDeletedService;




    @GetMapping
    public String usersPage(HttpSession session, Model model,
                            @RequestParam(required = false) String search) {
        User adminUser = authMiddlewareService.getAdminUser(session);

        List<User> users;
        if (search != null && !search.trim().isEmpty()) {
            users = userSearchService.searchUsers(search.trim());
        } else {
            users = userSearchService.getAllUsers(adminUser);
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

    @PostMapping("/change-role")
    public String changeRole(@Valid @ModelAttribute ChangeRoleRequest request,
                             BindingResult bindingResult,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User adminUser = authMiddlewareService.getAdminUser(session);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Ошибка валидации");
            return "redirect:/admin/users";
        }

        try {
            User updatedUser = userRoleService.changeUserRole(
                    adminUser, request.getUserId(), request.getNewRole());

            redirectAttributes.addFlashAttribute("success",
                    "Роль пользователя " + updatedUser.getEmail() + " изменена на " + request.getNewRole());

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/toggle-active/{userId}")
    public String toggleActive(@PathVariable Long userId,
                               @RequestParam boolean active,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User adminUser = authMiddlewareService.getAdminUser(session);

        try {
            User updatedUser = userStatusService.toggleUserActive(adminUser, userId, active);

            String message = active ?
                    "Пользователь " + updatedUser.getEmail() + " активирован" :
                    "Пользователь " + updatedUser.getEmail() + " деактивирован";

            redirectAttributes.addFlashAttribute("success", message);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/create")
    public String createUser(@Valid @ModelAttribute CreateUserRequest request,
                             BindingResult bindingResult,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User adminUser = authMiddlewareService.getAdminUser(session);


        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Ошибка валидации");
            return "redirect:/admin/users";
        }

        try {
            User newUser = User.builder()
                    .email(request.getEmail())
                    .password(request.getPassword())
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .surname(request.getSurname())
                    .birthDate(request.getBirthDate())
                    .role(request.getRole())
                    .active(request.isActive())
                    .build();

            User createdUser = adminUserCreationService.createUserWithRole(adminUser, newUser, request.getRole());

            redirectAttributes.addFlashAttribute("success",
                    "Пользователь " + createdUser.getEmail() + " создан с ролью " + request.getRole());

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/users";
    }

    // ========== УДАЛЕНИЕ ПОЛЬЗОВАТЕЛЯ ==========

    @PostMapping("/delete/{userId}")
    public String deleteUser(@PathVariable Long userId,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        try {
            User adminUser = authMiddlewareService.getAdminUser(session);

            // Нельзя удалить самого себя
            if (adminUser.getId().equals(userId)) {
                redirectAttributes.addFlashAttribute("error",
                        "Нельзя удалить свой собственный аккаунт");
                return "redirect:/admin/users";
            }

            // Получаем пользователя для удаления
            User userToDelete = userSearchService.getUserById(userId);


            // Проверяем права на удаление
            if (!adminUser.getRole().canManage(userToDelete.getRole())) {
                redirectAttributes.addFlashAttribute("error",
                        "Недостаточно прав для удаления пользователя с ролью " + userToDelete.getRole());
                return "redirect:/admin/users";
            }

            // Удаляем пользователя
            userDeletedService.deleteUser(userId);

            redirectAttributes.addFlashAttribute("success",
                    "Пользователь " + userToDelete.getEmail() + " успешно удален");

        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", "Пользователь не найден");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }



}