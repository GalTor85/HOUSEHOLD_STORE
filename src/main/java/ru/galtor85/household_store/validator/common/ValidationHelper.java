package ru.galtor85.household_store.validator.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.order.CannotReceivePurchaseOrderException;
import ru.galtor85.household_store.advice.exception.order.WriteOffInsufficientStockException;
import ru.galtor85.household_store.advice.exception.supplier.SupplierAlreadyExistsException;
import ru.galtor85.household_store.advice.exception.supplier.SupplierInactiveException;
import ru.galtor85.household_store.advice.exception.supplier.SupplierProductAlreadyExistsException;
import ru.galtor85.household_store.advice.exception.validation.InvalidDateRangeException;
import ru.galtor85.household_store.dto.request.supplier.SupplierCreateRequest;
import ru.galtor85.household_store.dto.request.supplier.SupplierUpdateRequest;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.supplier.Supplier;
import ru.galtor85.household_store.entity.supplier.SupplierStatus;
import ru.galtor85.household_store.repository.supplier.SupplierProductRepository;
import ru.galtor85.household_store.repository.supplier.SupplierRepository;
import ru.galtor85.household_store.resolver.WarehouseResolver;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ValidationHelper {

    private final SupplierRepository supplierRepository;
    private final SupplierProductRepository supplierProductRepository;
    private final MessageService messageService;
    private final WarehouseResolver warehouseResolver;

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

    public void validateOrderForReceiving(SalesOrder salesOrder) {
        if (salesOrder.getStatus() != OrderStatus.PROCESSING && salesOrder.getStatus() != OrderStatus.PENDING) {
            warehouseResolver.resolveWarehouseId(salesOrder);
            log.error(messageService.get("manager.purchase.log.cannot.receive", salesOrder.getStatus()));
            throw new CannotReceivePurchaseOrderException(salesOrder.getStatus());
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