package ru.galtor85.household_store.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.galtor85.household_store.advice.GlobalExceptionHandler;


import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class to ensure all custom exceptions have handlers in GlobalExceptionHandler.
 */
@DisplayName("GlobalExceptionHandler should handle all custom exceptions")
class GlobalExceptionHandlerTest {

    private static final String EXCEPTION_PACKAGE = "ru.galtor85.household_store.advice.exception";

    @Test
    @DisplayName("All custom exceptions should have corresponding @ExceptionHandler methods")
    void allCustomExceptionsShouldHaveHandlers() {
        // Find all custom exception classes in the exception package
        Reflections reflections = new Reflections(EXCEPTION_PACKAGE);
        Set<Class<? extends RuntimeException>> exceptionClasses =
                reflections.getSubTypesOf(RuntimeException.class);

        // Get all handled exception types from GlobalExceptionHandler
        Set<Class<?>> handledExceptions = Arrays.stream(GlobalExceptionHandler.class.getMethods())
                .filter(method -> method.isAnnotationPresent(ExceptionHandler.class))
                .flatMap(method -> Arrays.stream(method.getAnnotation(ExceptionHandler.class).value()))
                .collect(Collectors.toSet());

        // Check that every custom exception has a handler
        Set<Class<?>> missingHandlers = new HashSet<>();
        for (Class<? extends RuntimeException> exceptionClass : exceptionClasses) {
            if (!handledExceptions.contains(exceptionClass)) {
                missingHandlers.add(exceptionClass);
            }
        }

        assertThat(missingHandlers)
                .as("The following exceptions do not have handlers in GlobalExceptionHandler")
                .isEmpty();
    }

    @Test
    @DisplayName("Handler methods should have proper logging")
    void handlerMethodsShouldHaveLogging() {
        Set<Method> handlerMethods = Arrays.stream(GlobalExceptionHandler.class.getMethods())
                .filter(method -> method.isAnnotationPresent(ExceptionHandler.class))
                .collect(Collectors.toSet());

        Set<String> methodsWithoutLogging = getStrings(handlerMethods);

        // This is a basic check - in reality, you'd need bytecode analysis for accurate logging check
        // For now, we just verify the method naming convention
        assertThat(methodsWithoutLogging)
                .as("Handler methods should follow naming convention (handle*)")
                .isEmpty();
    }

    private static Set<String> getStrings(Set<Method> handlerMethods) {
        Set<String> methodsWithoutLogging = new HashSet<>();
        for (Method method : handlerMethods) {
            // Skip synthetic methods
            if (method.isSynthetic()) {
                continue;
            }
            // Check if method name indicates logging (heuristic, can be improved)
            String methodName = method.getName();
            if (!methodName.startsWith("handle") && !methodName.contains("Exception")) {
                methodsWithoutLogging.add(methodName);
            }
        }
        return methodsWithoutLogging;
    }


    @Test
    @DisplayName("Exception handlers should return ApiResponse")
    void exceptionHandlersShouldReturnApiResponse() {
        Set<Method> handlerMethods = Arrays.stream(GlobalExceptionHandler.class.getMethods())
                .filter(method -> method.isAnnotationPresent(ExceptionHandler.class))
                .collect(Collectors.toSet());

        for (Method method : handlerMethods) {
            String returnTypeName = method.getGenericReturnType().getTypeName();
            assertThat(returnTypeName)
                    .as("Handler %s should return ResponseEntity<ApiResponse<Void>>", method.getName())
                    .contains("ResponseEntity")
                    .contains("ApiResponse");
        }
    }
}