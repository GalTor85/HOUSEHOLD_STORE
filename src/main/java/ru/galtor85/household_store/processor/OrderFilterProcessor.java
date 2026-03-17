package ru.galtor85.household_store.processor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.Order;
import ru.galtor85.household_store.entity.OrderType;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderFilterProcessor {

    public Page<Order> filterCustomerOrders(Page<Order> ordersPage, Pageable pageable) {
        List<Order> filteredOrders = ordersPage.getContent().stream()
                .filter(order -> order.getOrderType() == OrderType.RETAIL ||
                        order.getOrderType() == OrderType.WHOLESALE)
                .collect(Collectors.toList());

        return new PageImpl<>(
                filteredOrders,
                pageable,
                filteredOrders.size()
        );
    }
}