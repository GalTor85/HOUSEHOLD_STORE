package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.builder.SalesOrderBuilder;
import ru.galtor85.household_store.dto.CartItemDto;
import ru.galtor85.household_store.dto.PriceCalculationRequest;
import ru.galtor85.household_store.dto.PriceCalculationResult;
import ru.galtor85.household_store.dto.SalesOrderCreateRequest;
import ru.galtor85.household_store.entity.*;
import ru.galtor85.household_store.repository.SalesOrderRepository;
import ru.galtor85.household_store.service.MessageService;
import ru.galtor85.household_store.service.PriceCalculationService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SalesOrderProcessor {

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderBuilder builder;
    private final PriceCalculationService priceCalculationService;
    private final MessageService messageService;

    /**
     * Создает заказ на продажу из корзины
     */
    @Transactional
    public SalesOrder createOrderFromCart(Cart cart, String shippingAddress, Long userId) {

        log.info(messageService.get("sales.order.processor.create.from.cart.start", userId));

        // 1. Конвертируем товары корзины в DTO для расчета
        List<CartItemDto> items = cart.getItems().stream()
                .map(this::convertToCartItemDto)
                .collect(Collectors.toList());

        // 2. Рассчитываем цены со скидками
        PriceCalculationRequest priceRequest = PriceCalculationRequest.builder()
                .userId(userId)
                .items(items)
                .shippingAddress(shippingAddress)
                .applyUserTypeDiscounts(true)
                .applyPromoCode(true)
                .applyPriceRules(true)
                .build();

        //
        PriceCalculationResult priceResult = priceCalculationService.calculatePrice(priceRequest);

        // 3. Создаем запрос для билдера
        SalesOrderCreateRequest createRequest = SalesOrderCreateRequest.builder()
                .userId(userId)
                .orderType(determineOrderType(userId))
                .shippingAddress(shippingAddress)
                .build();

        // 4. Создаем заказ через билдер
        SalesOrder order = builder.buildOrder(createRequest, userId);

        // 5. Устанавливаем рассчитанные суммы
        order.setStatus(OrderStatus.PENDING);
        order.setSubtotal(priceResult.getOriginalTotal());
        order.setDiscountAmount(priceResult.getTotalDiscount());
        order.setTotalAmount(priceResult.getFinalTotal());

        // 6. Добавляем позиции заказа
        for (CartItem cartItem : cart.getItems()) {
            SalesOrderItem orderItem = SalesOrderItem.builder()
                    .salesOrder(order)
                    .productId(cartItem.getProductId())
                    .quantity(cartItem.getQuantity())
                    .price(cartItem.getPrice())
                    .productName(cartItem.getProductName())
                    .productSku(cartItem.getSku())
                    .build();
            order.addItem(orderItem);
        }

        // 7. Сохраняем заказ
        SalesOrder savedOrder = salesOrderRepository.save(order);

        log.info(messageService.get("sales.order.processor.create.from.cart.complete",
                savedOrder.getOrderNumber(), userId, priceResult.getFinalTotal()));

        return savedOrder;
    }

    /**
     * Создает заказ на продажу из запроса (для прямого создания менеджером)
     */
    @Transactional
    public SalesOrder createSalesOrder(SalesOrderCreateRequest request,
                                       List<Product> products,
                                       List<BigDecimal> prices,
                                       Long userId) {

        log.info(messageService.get("sales.order.processor.create.start", userId));

        // 1. Создаем заказ через билдер
        SalesOrder order = builder.buildOrder(request, userId);

        // 2. Создаем позиции через билдер
        List<SalesOrderItem> items = builder.buildOrderItems(
                order,
                request.getItems(),
                products,
                prices
        );

        // 3. Рассчитываем сумму
        BigDecimal totalAmount = builder.calculateTotalAmount(items);

        // 4. Устанавливаем позиции и суммы
        order.setItems(items);
        order.setSubtotal(totalAmount);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING);

        // 5. Сохраняем заказ
        SalesOrder savedOrder = salesOrderRepository.save(order);

        log.info(messageService.get("sales.order.processor.create.complete",
                savedOrder.getOrderNumber(), userId, items.size(), totalAmount));

        return savedOrder;
    }

    /**
     * Конвертирует CartItem в CartItemDto
     */
    private CartItemDto convertToCartItemDto(CartItem cartItem) {
        return CartItemDto.builder()
                .productId(cartItem.getProductId())
                .productName(cartItem.getProductName())
                .sku(cartItem.getSku())
                .quantity(cartItem.getQuantity())
                .price(cartItem.getPrice())
                .category(cartItem.getCategory())
                .totalPrice(cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                .build();
    }

    /**
     * Определяет тип заказа (розничный или оптовый)
     */
    private SalesOrderType determineOrderType(Long userId) {
        // TODO: Реализовать логику определения типа пользователя
        // Например, проверить UserTypeAssignment
        return SalesOrderType.RETAIL;
    }
}