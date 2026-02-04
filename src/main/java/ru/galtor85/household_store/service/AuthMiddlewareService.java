package ru.galtor85.household_store.service;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;


@Service
@RequiredArgsConstructor
public class AuthMiddlewareService {



    public User getAdminUser(HttpSession session) {
        User user = getUserFromSession(session);
        requireAdminOrManager(user);
        return user;
    }

    public User getUserWithRequiredRole(HttpSession session, Role requiredRole) {
        User user = getUserFromSession(session);
        if (user.getRole() != requiredRole) {
            throw new AccessDeniedException(
                    String.format("Доступ запрещен. Требуется роль %s", requiredRole));
        }
        return user;
    }

    public User getAuthenticatedUser(HttpSession session) {
        return getUserFromSession(session);
    }

    public void requireAdminOrManager(User user) {
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.MANAGER) {
            throw new AccessDeniedException("Доступ запрещен. Требуется роль ADMIN или MANAGER");
        }
    }

    public void requireRole(User user, Role requiredRole) {
        if (user.getRole() != requiredRole) {
            throw new AccessDeniedException(
                    String.format("Требуется роль %s", requiredRole));
        }
    }

    private User getUserFromSession(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("Требуется авторизация");
        }
        return user;
    }
}