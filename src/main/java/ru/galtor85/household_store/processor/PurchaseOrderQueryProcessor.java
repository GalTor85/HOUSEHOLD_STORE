package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.OrderDto;
import ru.galtor85.household_store.entity.Order;
import ru.galtor85.household_store.entity.OrderStatus;
import ru.galtor85.household_store.entity.OrderType;
import ru.galtor85.household_store.mapper.OrderMapper;
import ru.galtor85.household_store.repository.OrderRepository;
import ru.galtor85.household_store.service.MessageService;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseOrderQueryProcessor {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final MessageService messageService;

    public Page<OrderDto> getPurchaseOrders(Long supplierId, OrderStatus status,
                                            LocalDateTime start, LocalDateTime end,
                                            Pageable pageable) {

        Page<Order> orders = orderRepository.searchOrders(
                null, supplierId, status, OrderType.PURCHASE, start, end, pageable);

        log.debug(messageService.get("manager.purchase.fetched.log", orders.getTotalElements()));

        return orders.map(orderMapper::toDto);
    }
}