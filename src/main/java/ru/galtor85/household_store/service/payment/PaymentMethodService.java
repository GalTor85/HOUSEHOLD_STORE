package ru.galtor85.household_store.service.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.converter.PaymentMethodConverter;
import ru.galtor85.household_store.dto.request.payment.CreatePaymentMethodWithTypesRequest;
import ru.galtor85.household_store.dto.response.payment.PaymentMethodForUserDto;
import ru.galtor85.household_store.dto.response.payment.PaymentMethodWithUserTypesDto;
import ru.galtor85.household_store.entity.payment.PaymentMethod;
import ru.galtor85.household_store.entity.payment.PaymentMethodUserType;
import ru.galtor85.household_store.entity.user.UserType;
import ru.galtor85.household_store.processor.payment.PaymentMethodProcessor;
import ru.galtor85.household_store.repository.payment.PaymentMethodRepository;
import ru.galtor85.household_store.repository.payment.PaymentMethodUserTypeRepository;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.validator.payment.PaymentMethodValidator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for payment method management.
 *
 * <p>This service provides comprehensive payment method management functionality:</p>
 * <ul>
 *   <li>Creating payment methods with user type assignments</li>
 *   <li>Assigning payment methods to user types (RETAIL, WHOLESALE, VIP, etc.)</li>
 *   <li>Retrieving payment methods with user type assignments</li>
 *   <li>Updating, activating, deactivating, and deleting payment methods</li>
 *   <li>Getting available payment methods for specific user types (customer view)</li>
 * </ul>
 *
 * <p>All manager methods require ADMIN or MANAGER role for access.</p>
 * <p>User methods are available for authenticated customers.</p>
 *
 * @author G@LTor85
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentMethodUserTypeRepository paymentMethodUserTypeRepository;
    private final PaymentMethodValidator validator;
    private final PaymentMethodProcessor processor;
    private final PaymentMethodConverter converter;
    private final MessageService messageService;

    // =========================================================================
    // MANAGER METHODS - CREATE WITH USER TYPES
    // =========================================================================

    /**
     * Creates a new payment method with user type assignments.
     *
     * <p>This method performs the following operations:</p>
     * <ol>
     *   <li>Validates the creation request</li>
     *   <li>Converts the request to a PaymentMethod entity</li>
     *   <li>Saves the payment method to the database</li>
     *   <li>Assigns the payment method to specified user types</li>
     *   <li>Returns the created payment method with assignments</li>
     * </ol>
     *
     * @param request the payment method creation request with user types
     * @param createdBy ID of the manager creating the method
     * @return created payment method DTO with user type assignments
     * @throws IllegalArgumentException if validation fails
     */
    @Transactional
    public PaymentMethodWithUserTypesDto createPaymentMethodWithUserTypes(
            CreatePaymentMethodWithTypesRequest request, Long createdBy) {

        log.info(messageService.get("payment.service.create.method.with.types.start",
                createdBy, request.getName(), request.getAvailableForUserTypes()));

        // Validate request using validator
        validator.validateCreateWithTypesRequest(request);

        // Convert request to entity using converter
        PaymentMethod paymentMethod = converter.toEntityWithTypes(request, createdBy);

        // Save payment method using processor
        PaymentMethod saved = processor.createPaymentMethod(paymentMethod);

        // Assign to user types
        if (request.getAvailableForUserTypes() != null && !request.getAvailableForUserTypes().isEmpty()) {
            assignPaymentMethodToUserTypes(
                    saved.getId(),
                    request.getAvailableForUserTypes(),
                    request.getSortOrder(),
                    createdBy
            );
        }

        log.info(messageService.get("payment.service.create.method.with.types.success",
                saved.getId(), request.getAvailableForUserTypes().size()));

        return getPaymentMethodWithUserTypes(saved.getId());
    }

    // =========================================================================
    // MANAGER METHODS - ASSIGN USER TYPES
    // =========================================================================

    /**
     * Assigns payment method to user types.
     *
     * <p>This method replaces all existing assignments for the payment method
     * with the new set of user types. The sort order determines display priority.</p>
     *
     * @param paymentMethodId payment method ID
     * @param userTypes set of user types (RETAIL, WHOLESALE, VIP, PARTNER, EMPLOYEE)
     * @param sortOrder sort order for display (lower number = higher priority)
     * @param assignedBy ID of the manager performing the assignment
     */
    @Transactional
    public void assignPaymentMethodToUserTypes(Long paymentMethodId, Set<UserType> userTypes,
                                               Integer sortOrder, Long assignedBy) {

        log.info(messageService.get("payment.service.assign.method.to.types.start",
                paymentMethodId, userTypes));

        // Remove existing assignments using repository
        paymentMethodUserTypeRepository.deleteByPaymentMethodId(paymentMethodId);

        // Create new assignments
        int order = sortOrder != null ? sortOrder : 0;
        for (UserType userType : userTypes) {
            PaymentMethodUserType assignment = PaymentMethodUserType.builder()
                    .paymentMethodId(paymentMethodId)
                    .userType(userType)
                    .active(true)
                    .sortOrder(order)
                    .createdBy(assignedBy)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            paymentMethodUserTypeRepository.save(assignment);
        }

        log.info(messageService.get("payment.service.assign.method.to.types.success",
                paymentMethodId, userTypes.size()));
    }

    // =========================================================================
    // MANAGER METHODS - GET
    // =========================================================================

    /**
     * Gets payment method with user type assignments.
     *
     * <p>Retrieves a payment method by ID and enriches it with all user types
     * that have access to this payment method.</p>
     *
     * @param paymentMethodId payment method ID
     * @return payment method DTO with user type assignments
     * @throws IllegalArgumentException if payment method not found
     */
    @Transactional(readOnly = true)
    public PaymentMethodWithUserTypesDto getPaymentMethodWithUserTypes(Long paymentMethodId) {
        log.debug(messageService.get("payment.service.get.method.with.types.start", paymentMethodId));

        // Validate payment method exists using validator
        PaymentMethod paymentMethod = validator.validatePaymentMethodExists(paymentMethodId);

        // Get assignments from repository
        List<PaymentMethodUserType> assignments = paymentMethodUserTypeRepository
                .findByPaymentMethodId(paymentMethodId);

        // Extract user types from assignments
        Set<UserType> userTypes = assignments.stream()
                .map(PaymentMethodUserType::getUserType)
                .collect(Collectors.toSet());

        // Get sort order (use first assignment's order or default 0)
        Integer sortOrder = assignments.isEmpty() ? 0 : assignments.get(0).getSortOrder();

        log.debug(messageService.get("payment.service.get.method.with.types.success",
                paymentMethodId, userTypes.size()));

        // Convert to DTO using converter
        return converter.toDtoWithUserTypes(paymentMethod, userTypes, sortOrder);
    }

    /**
     * Gets all payment methods with user type assignments.
     *
     * <p>Retrieves all payment methods in the system and enriches each with
     * its assigned user types.</p>
     *
     * @return list of all payment methods with user types
     */
    @Transactional(readOnly = true)
    public List<PaymentMethodWithUserTypesDto> getAllPaymentMethodsWithUserTypes() {
        log.debug(messageService.get("payment.service.get.all.methods.start"));

        List<PaymentMethod> methods = paymentMethodRepository.findAll();

        log.debug(messageService.get("payment.service.get.all.methods.count", methods.size()));

        return methods.stream()
                .map(method -> {
                    List<PaymentMethodUserType> assignments = paymentMethodUserTypeRepository
                            .findByPaymentMethodId(method.getId());
                    Set<UserType> userTypes = assignments.stream()
                            .map(PaymentMethodUserType::getUserType)
                            .collect(Collectors.toSet());
                    Integer sortOrder = assignments.isEmpty() ? 0 : assignments.get(0).getSortOrder();
                    return converter.toDtoWithUserTypes(method, userTypes, sortOrder);
                })
                .collect(Collectors.toList());
    }

    /**
     * Gets payment methods by user type (manager view).
     *
     * <p>Returns all payment methods available for a specific user type,
     * enriched with full details including assignments and sort order.</p>
     *
     * @param userType the user type (RETAIL, WHOLESALE, VIP, PARTNER, EMPLOYEE)
     * @return list of payment methods available for the user type
     */
    @Transactional(readOnly = true)
    public List<PaymentMethodWithUserTypesDto> getPaymentMethodsByUserType(UserType userType) {
        log.debug(messageService.get("payment.service.get.methods.by.type.start", userType));

        // Get payment method IDs for this user type from repository
        List<Long> methodIds = paymentMethodUserTypeRepository
                .findActivePaymentMethodIdsByUserType(userType);

        log.debug(messageService.get("payment.service.get.methods.by.type.ids", methodIds.size(), userType));

        // Fetch payment methods by IDs
        List<PaymentMethod> methods = paymentMethodRepository.findAllById(methodIds);

        return methods.stream()
                .map(method -> {
                    List<PaymentMethodUserType> assignments = paymentMethodUserTypeRepository
                            .findByPaymentMethodId(method.getId());
                    Set<UserType> userTypes = assignments.stream()
                            .map(PaymentMethodUserType::getUserType)
                            .collect(Collectors.toSet());
                    Integer sortOrder = assignments.isEmpty() ? 0 : assignments.get(0).getSortOrder();
                    return converter.toDtoWithUserTypes(method, userTypes, sortOrder);
                })
                .collect(Collectors.toList());
    }

    // =========================================================================
    // MANAGER METHODS - UPDATE
    // =========================================================================

    /**
     * Updates a payment method.
     *
     * <p>Updates payment method details and optionally updates user type
     * assignments if provided in the request.</p>
     *
     * @param methodId payment method ID
     * @param request update request with new values
     * @return updated payment method DTO with user type assignments
     * @throws IllegalArgumentException if payment method not found
     */
    @Transactional
    public PaymentMethodWithUserTypesDto updatePaymentMethod(Long methodId,
                                                             CreatePaymentMethodWithTypesRequest request) {
        log.info(messageService.get("payment.service.update.method.start", methodId));

        // Validate payment method exists
        PaymentMethod paymentMethod = validator.validatePaymentMethodExists(methodId);

        // Update entity using converter
        converter.updateEntityWithTypes(paymentMethod, request);

        // Save using processor
        PaymentMethod updated = processor.updatePaymentMethod(paymentMethod);

        // Update user type assignments if provided
        if (request.getAvailableForUserTypes() != null) {
            assignPaymentMethodToUserTypes(methodId, request.getAvailableForUserTypes(),
                    request.getSortOrder(), paymentMethod.getCreatedBy());
        }

        log.info(messageService.get("payment.service.update.method.success", methodId));

        return getPaymentMethodWithUserTypes(methodId);
    }

    /**
     * Deactivates a payment method.
     *
     * <p>Deactivated payment methods are not visible to users and cannot be
     * used for new transactions.</p>
     *
     * @param methodId payment method ID
     * @return updated payment method DTO with user type assignments
     * @throws IllegalArgumentException if payment method not found
     */
    @Transactional
    public PaymentMethodWithUserTypesDto deactivatePaymentMethod(Long methodId) {
        log.info(messageService.get("payment.service.deactivate.method.start", methodId));

        // Validate payment method exists
        PaymentMethod paymentMethod = validator.validatePaymentMethodExists(methodId);

        // Deactivate using processor
        paymentMethod.setActive(false);
        PaymentMethod updated = processor.updatePaymentMethod(paymentMethod);

        log.info(messageService.get("payment.service.deactivate.method.success", methodId));

        return getPaymentMethodWithUserTypes(methodId);
    }

    /**
     * Activates a payment method.
     *
     * <p>Activated payment methods become visible to users and can be used
     * for new transactions.</p>
     *
     * @param methodId payment method ID
     * @return updated payment method DTO with user type assignments
     * @throws IllegalArgumentException if payment method not found
     */
    @Transactional
    public PaymentMethodWithUserTypesDto activatePaymentMethod(Long methodId) {
        log.info(messageService.get("payment.service.activate.method.start", methodId));

        // Validate payment method exists
        PaymentMethod paymentMethod = validator.validatePaymentMethodExists(methodId);

        // Activate using processor
        paymentMethod.setActive(true);
        PaymentMethod updated = processor.updatePaymentMethod(paymentMethod);

        log.info(messageService.get("payment.service.activate.method.success", methodId));

        return getPaymentMethodWithUserTypes(methodId);
    }

    /**
     * Deletes a payment method permanently.
     *
     * <p>This operation also removes all user type assignments associated
     * with the payment method.</p>
     *
     * @param methodId payment method ID
     * @throws IllegalArgumentException if payment method not found
     */
    @Transactional
    public void deletePaymentMethod(Long methodId) {
        log.info(messageService.get("payment.service.delete.method.start", methodId));

        // Validate payment method exists
        PaymentMethod paymentMethod = validator.validatePaymentMethodExists(methodId);

        // Delete user type assignments first (using repository)
        paymentMethodUserTypeRepository.deleteByPaymentMethodId(methodId);

        // Delete payment method using processor
        paymentMethodRepository.delete(paymentMethod);

        log.info(messageService.get("payment.service.delete.method.success", methodId));
    }

    // =========================================================================
    // USER METHODS (CUSTOMER VIEW)
    // =========================================================================

    /**
     * Gets available payment methods for a user type (user view).
     *
     * <p>Returns a safe view of payment methods without sensitive information.
     * Only active payment methods assigned to the user type are returned.</p>
     *
     * @param userType the user's type (RETAIL, WHOLESALE, VIP, etc.)
     * @return list of payment methods for the user (safe view)
     */
    @Transactional(readOnly = true)
    public List<PaymentMethodForUserDto> getPaymentMethodsForUserType(UserType userType) {
        log.debug(messageService.get("payment.service.get.user.methods.start", userType));

        // Get payment method IDs for this user type from repository
        List<Long> methodIds = paymentMethodUserTypeRepository
                .findActivePaymentMethodIdsByUserType(userType);

        log.debug(messageService.get("payment.service.get.user.methods.count", methodIds.size(), userType));

        // Fetch payment methods by IDs
        List<PaymentMethod> methods = paymentMethodRepository.findAllById(methodIds);

        // Convert to user-safe DTOs using converter
        return methods.stream()
                .filter(PaymentMethod::isActive)
                .map(converter::toUserDto)
                .collect(Collectors.toList());
    }

    /**
     * Gets payment method details for a user (user view).
     *
     * <p>Returns a safe view of a specific payment method after verifying
     * that it is available for the user's type.</p>
     *
     * @param methodId payment method ID
     * @param userType the user's type (for validation)
     * @return payment method DTO for user (safe view)
     * @throws IllegalArgumentException if payment method not available for user type
     * @throws IllegalStateException if payment method is inactive
     */
    @Transactional(readOnly = true)
    public PaymentMethodForUserDto getPaymentMethodForUserType(Long methodId, UserType userType) {
        log.debug(messageService.get("payment.service.get.user.method.start", methodId, userType));

        // Check if payment method is available for this user type using repository
        boolean isAvailable = paymentMethodUserTypeRepository
                .findByPaymentMethodIdAndUserType(methodId, userType)
                .isPresent();

        if (!isAvailable) {
            log.warn(messageService.get("payment.method.not.available.for.user.type.warn", methodId, userType));
            throw new IllegalArgumentException(
                    messageService.get("payment.method.not.available.for.user.type", methodId, userType));
        }

        // Validate payment method exists and is active
        PaymentMethod paymentMethod = validator.validatePaymentMethodExists(methodId);

        if (!paymentMethod.isActive()) {
            log.warn(messageService.get("payment.method.inactive.warn", methodId));
            throw new IllegalStateException(
                    messageService.get("payment.method.inactive", methodId));
        }

        log.debug(messageService.get("payment.service.get.user.method.success", methodId, userType));

        // Convert to user-safe DTO using converter
        return converter.toUserDto(paymentMethod);
    }
}