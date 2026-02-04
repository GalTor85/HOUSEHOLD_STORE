package ru.galtor85.household_store.advice;


import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.galtor85.household_store.controller.AdminApiController;
import ru.galtor85.household_store.controller.RoleBasedDashboardController;
import ru.galtor85.household_store.controller.AdminUserController;

@ControllerAdvice(assignableTypes = {
        ru.galtor85.household_store.controller.AdminController.class,
        AdminUserController.class,
        RoleBasedDashboardController.class,
        AdminApiController.class
})
public class AdminExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException e,
                                     RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
        return "redirect:/login";
    }

    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(RuntimeException e,
                                         RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
        return "redirect:/admin";
    }
}