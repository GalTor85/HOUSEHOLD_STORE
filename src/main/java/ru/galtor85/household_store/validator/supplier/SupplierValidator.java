package ru.galtor85.household_store.validator.supplier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.supplier.SupplierAlreadyExistsException;
import ru.galtor85.household_store.advice.exception.supplier.SupplierNotFoundException;
import ru.galtor85.household_store.dto.request.supplier.SupplierCreateRequest;
import ru.galtor85.household_store.dto.request.supplier.SupplierUpdateRequest;
import ru.galtor85.household_store.entity.supplier.Supplier;
import ru.galtor85.household_store.repository.supplier.SupplierRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;

/**
 * Validator for supplier operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SupplierValidator {

    private final SupplierRepository supplierRepository;
    private final LogMessageService logMsg;

    /**
     * Validates supplier exists by ID.
     *
     * @param supplierId supplier ID
     * @return supplier entity
     * @throws SupplierNotFoundException if not found
     */
    public Supplier validateExists(Long supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> {
                    log.error(logMsg.get("manager.supplier.error.not.found", supplierId));
                    return new SupplierNotFoundException(supplierId);
                });
    }

    /**
     * Validates uniqueness of email and INN on creation.
     *
     * @param request creation request
     * @throws SupplierAlreadyExistsException if email or INN already exists
     */
    public void validateUniqueness(SupplierCreateRequest request) {
        validateEmailUnique(request.getEmail(), null);
        validateInnUnique(request.getInn(), null);
    }

    /**
     * Validates uniqueness of email and INN on update.
     *
     * @param supplier existing supplier
     * @param request update request
     * @throws SupplierAlreadyExistsException if email or INN already exists
     */
    public void validateUniquenessOnUpdate(Supplier supplier, SupplierUpdateRequest request) {
        validateEmailUnique(request.getEmail(), supplier.getEmail());
        validateInnUnique(request.getInn(), supplier.getInn());
    }

    private void validateEmailUnique(String email, String currentEmail) {
        if (email == null || email.isEmpty() || email.equals(currentEmail)) {
            return;
        }
        if (supplierRepository.existsByEmail(email)) {
            log.warn(logMsg.get("manager.supplier.error.email.exists", email));
            throw new SupplierAlreadyExistsException("email", email);
        }
    }

    private void validateInnUnique(String inn, String currentInn) {
        if (inn == null || inn.isEmpty() || inn.equals(currentInn)) {
            return;
        }
        if (supplierRepository.existsByInn(inn)) {
            log.warn(logMsg.get("manager.supplier.error.inn.exists", inn));
            throw new SupplierAlreadyExistsException("inn", inn);
        }
    }
}