package ru.galtor85.household_store.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.*;
import ru.galtor85.household_store.dto.SupplierCreateRequest;
import ru.galtor85.household_store.dto.SupplierUpdateRequest;
import ru.galtor85.household_store.entity.*;
import ru.galtor85.household_store.repository.SupplierProductRepository;
import ru.galtor85.household_store.repository.SupplierRepository;
import ru.galtor85.household_store.service.MessageService;

import java.time.LocalDateTime;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class ValidationHelper {

    private final SupplierRepository supplierRepository;
    private final SupplierProductRepository supplierProductRepository;
    private final MessageService messageService;

    public void validateSupplierUniqueness(SupplierCreateRequest request) {
        if (request.getEmail() != null && supplierRepository.existsByEmail(request.getEmail())) {
            log.warn(messageService.get("manager.supplier.log.email.exists", request.getEmail()));
            throw new SupplierAlreadyExistsException("email", request.getEmail());
        }

        if (request.getInn() != null && supplierRepository.existsByInn(request.getInn())) {
            log.warn(messageService.get("manager.supplier.log.inn.exists", request.getInn()));
            throw new SupplierAlreadyExistsException("inn", request.getInn());
        }
    }

    public void validateSupplierUniquenessOnUpdate(Supplier supplier, SupplierUpdateRequest request) {
        if (request.getEmail() != null && !request.getEmail().equals(supplier.getEmail())
                && supplierRepository.existsByEmail(request.getEmail())) {
            log.warn(messageService.get("manager.supplier.log.email.exists", request.getEmail()));
            throw new SupplierAlreadyExistsException("email", request.getEmail());
        }

        if (request.getInn() != null && !request.getInn().equals(supplier.getInn())
                && supplierRepository.existsByInn(request.getInn())) {
            log.warn(messageService.get("manager.supplier.log.inn.exists", request.getInn()));
            throw new SupplierAlreadyExistsException("inn", request.getInn());
        }
    }

    public void validateProductNotLinked(Long supplierId, Long productId) {
        if (supplierProductRepository.findBySupplierIdAndProductId(supplierId, productId).isPresent()) {
            log.warn(messageService.get("manager.supplier.log.product.already.added", productId, supplierId));
            throw new SupplierProductAlreadyExistsException(productId, supplierId);
        }
    }

    public void validateSupplierActive(Supplier supplier) {
        if (supplier.getStatus() != SupplierStatus.ACTIVE) {
            log.error(messageService.get("manager.supplier.log.inactive", supplier.getStatus()));
            throw new SupplierInactiveException(supplier.getStatus());
        }
    }

    public void validateOrderForReceiving(Order order) {
        if (order.getStatus() != OrderStatus.PROCESSING && order.getStatus() != OrderStatus.PENDING) {
            log.error(messageService.get("manager.purchase.log.cannot.receive", order.getStatus()));
            throw new CannotReceivePurchaseOrderException(order.getStatus());
        }
    }

    public void validateStockAvailability(Product product, int requestedQuantity) {
        if (product.getQuantityInStock() < requestedQuantity) {
            log.error(messageService.get("manager.writeoff.log.insufficient.stock",
                    product.getId(), product.getQuantityInStock(), requestedQuantity));
            throw new WriteOffInsufficientStockException(
                    product.getId(), product.getQuantityInStock(), requestedQuantity);
        }
    }

    public void validateDateRange(LocalDateTime start, LocalDateTime end,
                                  String startDate, String endDate) {
        if (start != null && end != null && start.isAfter(end)) {
            log.warn(messageService.get("manager.order.log.date.range", startDate, endDate));
            throw new InvalidDateRangeException(start, end);
        }
    }
}