package ru.galtor85.household_store.controller;

// ========== REST API ДЛЯ AJAX ==========

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.galtor85.household_store.dto.ChangeRoleRequest;
import ru.galtor85.household_store.dto.UserResponse;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.service.UserManagementService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminApiController {
    private final UserManagementService userManagementService;

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers(HttpSession session) {
        User adminUser = (User) session.getAttribute("user");
        if (adminUser == null || adminUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).build();
        }

        List<User> users = userManagementService.getAllUsers(adminUser);
        List<UserResponse> responses = users.stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<UserResponse> changeRole(@PathVariable Long userId,
                                                   @RequestBody ChangeRoleRequest request,
                                                   HttpSession session) {
        try {
            User adminUser = (User) session.getAttribute("user");
            if (adminUser == null || adminUser.getRole() != Role.ADMIN) {
                return ResponseEntity.status(403).build();
            }

            User updatedUser = userManagementService.changeUserRole(
                    adminUser, userId, request.getNewRole());

            return ResponseEntity.ok(UserResponse.fromEntity(updatedUser));

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

}