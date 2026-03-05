package ru.galtor85.household_store.advice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.galtor85.household_store.advice.exception.AuthenticationManagerException;
import ru.galtor85.household_store.advice.exception.CustomAuthenticationException;
import ru.galtor85.household_store.dto.ApiResponse;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e,
                                     RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
        log.error("AccessDeniedException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e,
                                         RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
        log.error("RuntimeException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(AuthenticationManagerException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationManagerException(AuthenticationManagerException e,
                                                       RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
        log.error("AuthenticationManagerException: {}", e.getMessage());
        return new ResponseEntity<>(ApiResponse.error(e.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(CustomAuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomAuthenticationException(CustomAuthenticationException e,
                                                      RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());

        return new ResponseEntity<>(ApiResponse.error(e.getMessage()), HttpStatus.UNAUTHORIZED);
    }


}
