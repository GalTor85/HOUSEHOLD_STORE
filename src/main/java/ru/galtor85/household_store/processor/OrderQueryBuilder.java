package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.params.OrderQueryParams;
import ru.galtor85.household_store.entity.Order;
import ru.galtor85.household_store.repository.OrderRepository;

@Component
@RequiredArgsConstructor
public class OrderQueryBuilder {

    private final OrderRepository orderRepository;

    public Page<Order> executeQuery(OrderQueryParams params, Pageable pageable) {

        // Все параметры (customerId + status + startDate + endDate)
        if (params.hasAllParams()) {
            return orderRepository.findByUserIdAndStatusAndCreatedAtBetween(
                    params.getCustomerId(), params.getStatus(),
                    params.getStartDate(), params.getEndDate(), pageable);
        }

        // CustomerId + status + startDate
        if (params.hasCustomerId() && params.hasStatus() && params.hasStartDate() && !params.hasEndDate()) {
            return orderRepository.findByUserIdAndStatusAndCreatedAtAfter(
                    params.getCustomerId(), params.getStatus(), params.getStartDate(), pageable);
        }

        // CustomerId + status + endDate
        if (params.hasCustomerId() && params.hasStatus() && params.hasEndDate() && !params.hasStartDate()) {
            return orderRepository.findByUserIdAndStatusAndCreatedAtBefore(
                    params.getCustomerId(), params.getStatus(), params.getEndDate(), pageable);
        }

        // CustomerId + startDate + endDate
        if (params.hasCustomerId() && params.hasAllDates() && !params.hasStatus()) {
            return orderRepository.findByUserIdAndCreatedAtBetween(
                    params.getCustomerId(), params.getStartDate(), params.getEndDate(), pageable);
        }

        // Status + startDate + endDate
        if (params.hasStatus() && params.hasAllDates() && !params.hasCustomerId()) {
            return orderRepository.findByStatusAndCreatedAtBetween(
                    params.getStatus(), params.getStartDate(), params.getEndDate(), pageable);
        }

        // CustomerId + status
        if (params.hasCustomerId() && params.hasStatus() && !params.hasStartDate() && !params.hasEndDate()) {
            return orderRepository.findByUserIdAndStatus(
                    params.getCustomerId(), params.getStatus(), pageable);
        }

        // CustomerId + startDate
        if (params.hasCustomerId() && params.hasStartDate() && !params.hasStatus() && !params.hasEndDate()) {
            return orderRepository.findByUserIdAndCreatedAtAfter(
                    params.getCustomerId(), params.getStartDate(), pageable);
        }

        // CustomerId + endDate
        if (params.hasCustomerId() && params.hasEndDate() && !params.hasStatus() && !params.hasStartDate()) {
            return orderRepository.findByUserIdAndCreatedAtBefore(
                    params.getCustomerId(), params.getEndDate(), pageable);
        }

        // Status + startDate
        if (params.hasStatus() && params.hasStartDate() && !params.hasCustomerId() && !params.hasEndDate()) {
            return orderRepository.findByStatusAndCreatedAtAfter(
                    params.getStatus(), params.getStartDate(), pageable);
        }

        // Status + endDate
        if (params.hasStatus() && params.hasEndDate() && !params.hasCustomerId() && !params.hasStartDate()) {
            return orderRepository.findByStatusAndCreatedAtBefore(
                    params.getStatus(), params.getEndDate(), pageable);
        }

        // Только startDate + endDate
        if (params.hasAllDates() && !params.hasCustomerId() && !params.hasStatus()) {
            return orderRepository.findByCreatedAtBetween(
                    params.getStartDate(), params.getEndDate(), pageable);
        }

        // Только customerId
        if (params.hasCustomerId() && !params.hasStatus() && !params.hasStartDate() && !params.hasEndDate()) {
            return orderRepository.findByUserId(params.getCustomerId(), pageable);
        }

        // Только status
        if (params.hasStatus() && !params.hasCustomerId() && !params.hasStartDate() && !params.hasEndDate()) {
            return orderRepository.findByStatus(params.getStatus(), pageable);
        }

        // Только startDate
        if (params.hasStartDate() && !params.hasCustomerId() && !params.hasStatus() && !params.hasEndDate()) {
            return orderRepository.findByCreatedAtAfter(params.getStartDate(), pageable);
        }

        // Только endDate
        if (params.hasEndDate() && !params.hasCustomerId() && !params.hasStatus() && !params.hasStartDate()) {
            return orderRepository.findByCreatedAtBefore(params.getEndDate(), pageable);
        }

        // Поиск по тексту (если есть)
        if (params.hasSearchTerm()) {
            return orderRepository.searchByTerm(params.getSearchTerm(), pageable);
        }

        // По типу заказа (если есть)
        if (params.hasOrderType()) {
            return orderRepository.findByOrderType(params.getOrderType(), pageable);
        }

        // Все заказы
        return orderRepository.findAll(pageable);
    }
}

