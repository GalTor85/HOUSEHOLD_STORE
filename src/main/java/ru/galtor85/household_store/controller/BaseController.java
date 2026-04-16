package ru.galtor85.household_store.controller;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.galtor85.household_store.advice.exception.auth.CustomAuthenticationException;
import ru.galtor85.household_store.config.BusinessConfig;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.service.user.UserSearchService;

/**
 * Base controller for handling common business logic and security checks.
 */
@Setter
public abstract class BaseController {

    @Autowired
    protected BusinessConfig businessConfig;

    @Autowired
    protected MessageService messageService;

    @Autowired
    protected UserSearchService userSearchService;

    /**
     * Gets effective page number from request parameter or configuration default
     *
     * @param page request page parameter (can be null)
     * @return effective page number
     */
    protected int getPage(Integer page) {
        if (page != null && page >= 0) {
            return page;
        }
        Integer defaultPage = businessConfig.getPagination().getDefaultPage();
        return defaultPage != null ? defaultPage : 0;
    }

    /**
     * Gets effective page size from request parameter or configuration default
     *
     * @param size request size parameter (can be null)
     * @return effective page size (capped by max size)
     */
    protected int getSize(Integer size) {
        if (size != null && size > 0) {
            Integer maxSize = businessConfig.getPagination().getMaxSize();
            int max = maxSize != null ? maxSize : 100;
            return Math.min(size, max);
        }
        Integer defaultSize = businessConfig.getPagination().getDefaultSize();
        return defaultSize != null ? defaultSize : 20;
    }

    protected int getLowStockThreshold(Integer threshold) {
        if (threshold != null && threshold > 0) {
            return threshold;
        }
        Integer configThreshold = businessConfig.getStock().getLowStockThreshold();
        return configThreshold != null ? configThreshold : 10;
    }

    protected User getCurrentUser() {
        SecurityUser securityUser = getCurrentSecurityUser();
        return userSearchService.getUserById(securityUser.getUserId());
    }


    /**
     * Retrieves the currently authenticated SecurityUser.
     *
     * @return the SecurityUser instance
     * @throws CustomAuthenticationException if user is not authenticated
     */
    protected SecurityUser getCurrentSecurityUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomAuthenticationException(
                    messageService.get("auth.error.not.authenticated")
            );
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof SecurityUser) {
            return (SecurityUser) principal;
        }

        throw new CustomAuthenticationException(
                messageService.get("auth.error.invalid.principal")
        );
    }

    /**
     * Retrieves the ID of the currently authenticated user.
     *
     * @return current user ID
     * @throws CustomAuthenticationException if user is not authenticated
     */
    protected Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomAuthenticationException(
                    messageService.get("auth.error.not.authenticated")
            );
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof SecurityUser) {
            return ((SecurityUser) principal).getUserId();
        }

        throw new CustomAuthenticationException(
                messageService.get("currency.error.invalid.principal")
        );
    }
}