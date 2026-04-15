package ru.galtor85.household_store.validator.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.supplier.SupplierProductAlreadyExistsException;
import ru.galtor85.household_store.advice.exception.validation.InvalidDateRangeException;
import ru.galtor85.household_store.repository.supplier.SupplierProductRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;

import java.time.LocalDateTime;

/**
 * Common validation helper.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ValidationHelper {

    private final SupplierProductRepository supplierProductRepository;
    private final LogMessageService logMsg;

    /**
     * Validates product is not already linked to supplier.
     *
     * @param supplierId supplier ID
     * @param productId product ID
     * @throws SupplierProductAlreadyExistsException if already linked
     */
    public void validateProductNotLinked(Long supplierId, Long productId) {
        if (supplierProductRepository.findBySupplierIdAndProductId(supplierId, productId).isPresent()) {
            log.warn(logMsg.get("manager.supplier.log.product.already.added", productId, supplierId));
            throw new SupplierProductAlreadyExistsException(productId, supplierId);
        }
    }

    /**
     * Validates date range is valid (start before end).
     *
     * @param start start date
     * @param end end date
     * @param startDate original start date string for logging
     * @param endDate original end date string for logging
     * @throws InvalidDateRangeException if start is after end
     */
    public void validateDateRange(LocalDateTime start, LocalDateTime end,
                                  String startDate, String endDate) {
        if (start != null && end != null && start.isAfter(end)) {
            log.warn(logMsg.get("manager.order.log.date.range", startDate, endDate));
            throw new InvalidDateRangeException(start, end);
        }
    }
}