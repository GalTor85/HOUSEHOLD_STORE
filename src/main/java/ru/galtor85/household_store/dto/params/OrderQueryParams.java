package ru.galtor85.household_store.dto.params;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.galtor85.household_store.entity.OrderStatus;
import ru.galtor85.household_store.entity.OrderType;

import java.time.LocalDateTime;

/**
 * Параметры для поиска и фильтрации заказов
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderQueryParams {

    private Long customerId;
    private OrderStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String searchTerm;
    private OrderType orderType;

    public OrderQueryParams(Long customerId, OrderStatus status,
                            LocalDateTime startDate, LocalDateTime endDate) {
        this.customerId = customerId;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.orderType = OrderType.RETAIL; // значение по умолчанию
    }

    // ========== Фабричные методы ==========

    public static OrderQueryParams forCustomerOrders(Long customerId, OrderStatus status,
                                                     LocalDateTime start, LocalDateTime end) {
        return OrderQueryParams.builder()
                .customerId(customerId)
                .status(status)
                .startDate(start)
                .endDate(end)
                .orderType(OrderType.RETAIL)
                .build();
    }

    public static OrderQueryParams forPurchaseOrders(Long supplierId, OrderStatus status,
                                                     LocalDateTime start, LocalDateTime end) {
        return OrderQueryParams.builder()
                .customerId(supplierId)  // supplierId хранится в customerId для переиспользования
                .status(status)
                .startDate(start)
                .endDate(end)
                .orderType(OrderType.PURCHASE)
                .build();
    }

    // ========== Методы проверки наличия параметров ==========

    public boolean hasCustomerId() {
        return customerId != null;
    }

    public boolean hasStatus() {
        return status != null;
    }

    public boolean hasStartDate() {
        return startDate != null;
    }

    public boolean hasEndDate() {
        return endDate != null;
    }

    public boolean hasSearchTerm() {
        return searchTerm != null && !searchTerm.trim().isEmpty();
    }

    public boolean hasOrderType() {
        return orderType != null;
    }

    // ========== Комбинированные проверки ==========

    public boolean hasAllDates() {
        return hasStartDate() && hasEndDate();
    }

    public boolean hasCustomerAndStatus() {
        return hasCustomerId() && hasStatus();
    }

    public boolean hasCustomerAndDates() {
        return hasCustomerId() && hasAllDates();
    }

    public boolean hasStatusAndDates() {
        return hasStatus() && hasAllDates();
    }

    public boolean hasAllParams() {
        return hasCustomerId() && hasStatus() && hasAllDates();
    }

    // ========== Создание Pageable ==========

    public Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        return PageRequest.of(page, size, sort);
    }

    public Pageable createPageable(int page, int size, Sort sort) {
        return PageRequest.of(page, size, sort);
    }

    public Pageable createDefaultPageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by("createdAt").descending());
    }

    // ========== Валидация ==========

    public boolean isValidDateRange() {
        if (!hasAllDates()) {
            return true; // Если нет обеих дат, считаем валидным
        }
        return !startDate.isAfter(endDate);
    }

    public String getInvalidDateRangeMessage() {
        if (hasAllDates() && startDate.isAfter(endDate)) {
            return String.format("Start date %s is after end date %s", startDate, endDate);
        }
        return null;
    }
}