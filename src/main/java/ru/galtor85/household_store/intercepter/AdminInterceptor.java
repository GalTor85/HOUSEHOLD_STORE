package ru.galtor85.household_store.intercepter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;

@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        HttpSession session = request.getSession(false);
        if (session != null) {
            User user = (User) session.getAttribute("user");
            if (user != null &&
                    (user.getRole() == Role.ADMIN || user.getRole() == Role.MANAGER)) {
                return true;
            }
        }

        response.sendRedirect(request.getContextPath() + "/auth/login?error=admin");
        return false;
    }
}