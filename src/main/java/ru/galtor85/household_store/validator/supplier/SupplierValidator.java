package ru.galtor85.household_store.validator.supplier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.supplier.SupplierAlreadyExistsException;
import ru.galtor85.household_store.advice.exception.supplier.SupplierInactiveException;
import ru.galtor85.household_store.advice.exception.supplier.SupplierNotFoundException;
import ru.galtor85.household_store.dto.request.supplier.SupplierCreateRequest;
import ru.galtor85.household_store.dto.request.supplier.SupplierUpdateRequest;
import ru.galtor85.household_store.entity.supplier.Supplier;
import ru.galtor85.household_store.entity.supplier.SupplierStatus;
import ru.galtor85.household_store.repository.order.PurchaseOrderRepository;
import ru.galtor85.household_store.repository.supplier.SupplierRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupplierValidator {

    private final SupplierRepository supplierRepository;
    private final MessageService messageService;
    private final PurchaseOrderRepository purchaseOrderRepository;

    // =========================================================================
    // ПРОВЕРКА СУЩЕСТВОВАНИЯ
    // =========================================================================

    /**
     * Проверяет существование поставщика по ID
     */
    public Supplier validateExists(Long supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.supplier.error.not.found", supplierId));
                    return new SupplierNotFoundException(supplierId);
                });
    }

    /**
     * Проверяет существование поставщика и его активность
     */
    public Supplier validateActive(Long supplierId) {
        Supplier supplier = validateExists(supplierId);

        if (supplier.getStatus() != SupplierStatus.ACTIVE) {
            log.error(messageService.get("manager.purchase.error.supplier.inactive",
                    supplier.getStatus()));
            throw new SupplierInactiveException(supplier.getStatus());
        }

        return supplier;
    }

    // =========================================================================
    // ПРОВЕРКА УНИКАЛЬНОСТИ
    // =========================================================================

    /**
     * Проверяет уникальность email и ИНН при создании
     */
    public void validateUniqueness(SupplierCreateRequest request) {
        validateEmailUnique(request.getEmail(), null);
        validateInnUnique(request.getInn(), null);
    }

    /**
     * Проверяет уникальность email и ИНН при обновлении
     */
    public void validateUniquenessOnUpdate(Supplier supplier, SupplierUpdateRequest request) {
        validateEmailUnique(request.getEmail(), supplier.getEmail());
        validateInnUnique(request.getInn(), supplier.getInn());
    }

    /**
     * Проверяет уникальность email
     */
    private void validateEmailUnique(String email, String currentEmail) {
        if (email == null || email.isEmpty()) {
            return;
        }

        if (email.equals(currentEmail)) {
            return;
        }

        if (supplierRepository.existsByEmail(email)) {
            log.warn(messageService.get("manager.supplier.error.email.exists", email));
            throw new SupplierAlreadyExistsException("email", email);
        }
    }

    /**
     * Проверяет уникальность ИНН
     */
    private void validateInnUnique(String inn, String currentInn) {
        if (inn == null || inn.isEmpty()) {
            return;
        }

        if (inn.equals(currentInn)) {
            return;
        }

        if (supplierRepository.existsByInn(inn)) {
            log.warn(messageService.get("manager.supplier.error.inn.exists", inn));
            throw new SupplierAlreadyExistsException("inn", inn);
        }
    }

    // =========================================================================
    // ПРОВЕРКА СТАТУСА
    // =========================================================================

    /**
     * Проверяет, что поставщик активен
     */
    public void validateStatusActive(Supplier supplier) {
        if (supplier.getStatus() != SupplierStatus.ACTIVE) {
            log.error(messageService.get("manager.purchase.error.supplier.inactive",
                    supplier.getStatus()));
            throw new SupplierInactiveException(supplier.getStatus());
        }
    }

    /**
     * Проверяет, что поставщик может быть изменен (не заблокирован)
     */
    public void validateModifiable(Supplier supplier) {
        if (supplier.getStatus() == SupplierStatus.BLOCKED) {
            log.error(messageService.get("manager.supplier.error.blocked",
                    supplier.getId()));
            throw new IllegalStateException(
                    messageService.get("manager.supplier.error.blocked", supplier.getId())
            );
        }
    }

    // =========================================================================
    // ПРОВЕРКА ДАННЫХ
    // =========================================================================

    /**
     * Проверяет корректность контактных данных
     */
    public void validateContactInfo(SupplierCreateRequest request) {
        if (request.getEmail() == null && request.getPhone() == null) {
            log.warn(messageService.get("supplier.validation.contact.empty"));
            throw new IllegalArgumentException(
                    messageService.get("supplier.validation.contact.empty")
            );
        }
    }

    /**
     * Проверяет корректность банковских реквизитов
     */
    public void validateBankDetails(SupplierCreateRequest request) {
        // Если указан банковский счет, должны быть заполнены БИК и название банка
        if (request.getBankAccount() != null && !request.getBankAccount().isEmpty()) {
            if (request.getBankBic() == null || request.getBankBic().isEmpty()) {
                throw new IllegalArgumentException(
                        messageService.get("supplier.validation.bank.bic.required")
                );
            }
            if (request.getBankName() == null || request.getBankName().isEmpty()) {
                throw new IllegalArgumentException(
                        messageService.get("supplier.validation.bank.name.required")
                );
            }
        }
    }

    // =========================================================================
    // ПРОВЕРКА НА УДАЛЕНИЕ
    // =========================================================================

    /**
     * Проверяет, можно ли удалить поставщика
     */
    public void validateDeletable(Long supplierId) {
        Supplier supplier = validateExists(supplierId);

        // Проверяем, есть ли незавершенные заказы у поставщика
        boolean hasPendingOrders = purchaseOrderRepository.hasPendingOrders(supplierId);

        if (hasPendingOrders) {
            log.error(messageService.get("manager.supplier.error.has.pending.orders",
                    supplierId));
            throw new IllegalStateException(
                    messageService.get("manager.supplier.error.has.pending.orders", supplierId)
            );
        }

        // Проверяем, не заблокирован ли уже
        if (supplier.getStatus() == SupplierStatus.BLOCKED) {
            log.warn(messageService.get("manager.supplier.error.already.blocked",
                    supplierId));
            throw new IllegalStateException(
                    messageService.get("manager.supplier.error.already.blocked", supplierId)
            );
        }
    }
}