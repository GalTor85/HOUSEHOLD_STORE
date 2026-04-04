package ru.galtor85.household_store.processor.sales;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.builder.order.SalesOrderBuilder;
import ru.galtor85.household_store.dto.response.cart.CartItemDto;
import ru.galtor85.household_store.dto.request.price.PriceCalculationRequest;
import ru.galtor85.household_store.dto.response.finance.PriceCalculationResult;
import ru.galtor85.household_store.dto.request.order.SalesOrderCreateRequest;
import ru.galtor85.household_store.entity.cart.Cart;
import ru.galtor85.household_store.entity.cart.CartItem;
import ru.galtor85.household_store.entity.finance.Invoice;
import ru.galtor85.household_store.entity.finance.InvoiceStatus;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.entity.order.SalesOrderItem;
import ru.galtor85.household_store.entity.order.SalesOrderType;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.processor.invoice.InvoiceAutoCreationProcessor;
import ru.galtor85.household_store.repository.order.SalesOrderRepository;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.service.price.PriceCalculationService;
import ru.galtor85.household_store.validator.order.SalesOrderValidator;
import ru.galtor85.household_store.validator.order.SalesOrderValidationHelper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SalesOrderProcessor {

    // Репозитории
    private final SalesOrderRepository salesOrderRepository;

    // Билдеры
    private final SalesOrderBuilder builder;

    // Сервисы
    private final PriceCalculationService priceCalculationService;
    private final MessageService messageService;

    // Процессоры
    private final InvoiceAutoCreationProcessor invoiceAutoCreationProcessor;

    // Валидаторы
    private final SalesOrderValidator validator;
    private final SalesOrderValidationHelper validationHelper;

    // =========================================================================
    // СОЗДАНИЕ ЗАКАЗА ИЗ КОРЗИНЫ
    // =========================================================================

    /**
     * Создает заказ на продажу из корзины
     */
    @Transactional
    public SalesOrder createOrderFromCart(Cart cart, String shippingAddress, Long userId) {

        log.info(messageService.get("sales.order.processor.create.from.cart.start", userId));

        // 1. Валидация корзины
        validator.validateCartNotEmpty(cart);
        validator.validateCartItems(cart);

        // 2. Конвертируем товары корзины в DTO для расчета
        List<CartItemDto> items = convertCartItemsToDto(cart.getItems());

        // 3. Рассчитываем цены со скидками
        PriceCalculationRequest priceRequest = PriceCalculationRequest.builder()
                .userId(userId)
                .items(items)
                .shippingAddress(shippingAddress)
                .applyUserTypeDiscounts(true)
                .applyPromoCode(true)
                .applyPriceRules(true)
                .build();

        PriceCalculationResult priceResult = priceCalculationService.calculatePrice(priceRequest);

        // 4. Создаем запрос для билдера
        SalesOrderCreateRequest createRequest = SalesOrderCreateRequest.builder()
                .userId(userId)
                .orderType(determineOrderType(userId))
                .shippingAddress(shippingAddress)
                .build();

        // 5. Создаем заказ через билдер
        SalesOrder order = builder.buildOrder(createRequest, userId);

        // 6. Устанавливаем рассчитанные суммы
        order.setStatus(OrderStatus.PENDING);
        order.setSubtotal(priceResult.getOriginalTotal());
        order.setDiscountAmount(priceResult.getTotalDiscount());
        order.setTotalAmount(priceResult.getFinalTotal());

        // 7. Добавляем позиции заказа
        addOrderItems(order, cart.getItems());

        // 8. Сохраняем заказ
        SalesOrder savedOrder = salesOrderRepository.save(order);

        // 9. ✅ АВТОМАТИЧЕСКОЕ СОЗДАНИЕ СЧЕТА (через процессор)
        Invoice invoice = invoiceAutoCreationProcessor.createInvoiceForOrder(order, userId);
        if (invoice != null) {
            savedOrder.addInvoice(invoice);
            salesOrderRepository.save(savedOrder);
            log.info(messageService.get("sales.order.processor.invoice.created",
                    invoice.getInvoiceNumber(), savedOrder.getOrderNumber()));
        }

        log.info(messageService.get("sales.order.processor.create.from.cart.complete",
                savedOrder.getOrderNumber(), userId, priceResult.getFinalTotal()));

        return savedOrder;
    }

    // =========================================================================
    // ПРЯМОЕ СОЗДАНИЕ ЗАКАЗА (МЕНЕДЖЕРОМ)
    // =========================================================================

    /**
     * Создает заказ на продажу из запроса
     */
    @Transactional
    public SalesOrder createSalesOrder(SalesOrderCreateRequest request,
                                       List<Product> products,
                                       List<BigDecimal> prices,
                                       Long userId) {

        log.info(messageService.get("sales.order.processor.create.start", userId));

        // Валидация запроса
        validator.validateCreateRequest(request);
        validator.validateProducts(request.getItems());

        // Создаем заказ через билдер
        SalesOrder order = builder.buildOrder(request, userId);

        // Создаем позиции через билдер
        List<SalesOrderItem> items = builder.buildOrderItems(
                order,
                request.getItems(),
                products,
                prices
        );

        // Рассчитываем сумму
        BigDecimal subtotal = builder.calculateTotalAmount(items);

        // Рассчитываем итоговую сумму с учетом скидки, доставки и налогов
        BigDecimal discountAmount = request.getEffectiveDiscountAmount();
        BigDecimal shippingAmount = request.getEffectiveShippingAmount();
        BigDecimal taxAmount = request.getEffectiveTaxAmount();

        BigDecimal totalAmount = subtotal
                .add(shippingAmount)
                .add(taxAmount)
                .subtract(discountAmount);

        // Устанавливаем позиции и суммы
        order.setItems(items);order.setSubtotal(subtotal);
        order.setDiscountAmount(discountAmount);
        order.setShippingAmount(shippingAmount);
        order.setTaxAmount(taxAmount);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING);

        // Сохраняем заказ
        SalesOrder savedOrder = salesOrderRepository.save(order);

        // АВТОМАТИЧЕСКОЕ СОЗДАНИЕ СЧЕТА (через процессор)
        Invoice invoice = invoiceAutoCreationProcessor.createInvoiceForOrder(order, userId);
        if (invoice != null) {
            savedOrder.addInvoice(invoice);
            salesOrderRepository.save(savedOrder);
            log.info(messageService.get("sales.order.processor.invoice.created",
                    invoice.getInvoiceNumber(), savedOrder.getOrderNumber()));
        }

        log.info(messageService.get("sales.order.processor.create.complete",
                savedOrder.getOrderNumber(), userId, items.size(), totalAmount));

        return savedOrder;
    }

    @Transactional
    public SalesOrder createOrderFromCartWithPromo(Cart cart, String shippingAddress,
                                                   String promoCode, Long userId) {
        log.info(messageService.get("sales.order.processor.create.from.cart.promo.start", userId, promoCode));

        validator.validateCartNotEmpty(cart);
        validator.validateCartItems(cart);

        List<CartItemDto> items = convertCartItemsToDto(cart.getItems());

        PriceCalculationRequest priceRequest = PriceCalculationRequest.builder()
                .userId(userId)
                .items(items)
                .promoCode(promoCode)
                .shippingAddress(shippingAddress)
                .applyUserTypeDiscounts(true)
                .applyPromoCode(true)
                .applyPriceRules(true)
                .build();

        PriceCalculationResult priceResult = priceCalculationService.calculatePrice(priceRequest);

        SalesOrderCreateRequest createRequest = SalesOrderCreateRequest.builder()
                .userId(userId)
                .orderType(determineOrderType(userId))
                .shippingAddress(shippingAddress)
                .build();

        SalesOrder order = builder.buildOrder(createRequest, userId);
        order.setStatus(OrderStatus.PENDING);
        order.setSubtotal(priceResult.getOriginalTotal());
        order.setDiscountAmount(priceResult.getTotalDiscount());
        order.setTotalAmount(priceResult.getFinalTotal());

        addOrderItems(order, cart.getItems());
        SalesOrder savedOrder = salesOrderRepository.save(order);

        Invoice invoice = invoiceAutoCreationProcessor.createInvoiceForOrder(order, userId);
        if (invoice != null) {
            savedOrder.addInvoice(invoice);
            salesOrderRepository.save(savedOrder);
        }

        return savedOrder;
    }

    // =========================================================================
    // ОБНОВЛЕНИЕ ЗАКАЗА
    // =========================================================================

    /**
     * Обновляет заказ (позиции, суммы)
     */
    @Transactional
    public SalesOrder updateOrder(SalesOrder order, List<SalesOrderItem> newItems, Long updatedBy) {
        log.info(messageService.get("sales.order.processor.update.start", order.getOrderNumber()));

        // 1. Валидация возможности изменения
        validator.validateOrderModifiable(order);

        // 2. Обновляем позиции
        order.getItems().clear();
        for (SalesOrderItem item : newItems) {
            order.addItem(item);
        }

        // 3. Пересчитываем суммы
        order.recalculateTotals();

        // 4. Сохраняем
        SalesOrder updated = salesOrderRepository.save(order);

        // 5. ✅ Обновляем сумму счета, если он существует
        if (!order.getInvoices().isEmpty()) {
            Invoice invoice = order.getInvoices().get(0);
            if (invoice.getStatus() == InvoiceStatus.PENDING) {
                invoiceAutoCreationProcessor.updateInvoiceAmount(
                        invoice,
                        order.getTotalAmount(),
                        messageService.get("sales.order.processor.update.reason", updatedBy),
                        updatedBy
                );
            }
        }

        log.info(messageService.get("sales.order.processor.update.complete",
                order.getOrderNumber(), order.getTotalAmount()));

        return updated;
    }

    // =========================================================================
    // ОТМЕНА ЗАКАЗА
    // =========================================================================

    /**
     * Отменяет заказ
     */
    @Transactional
    public SalesOrder cancelOrder(SalesOrder order, String reason, Long cancelledBy) {
        log.info(messageService.get("sales.order.processor.cancel.start",
                order.getOrderNumber(), reason));

        // 1. Проверка возможности отмены
        validator.validateOrderCancellable(order);

        // 2. Обновляем статус
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancellationReason(reason);

        // 3. Отменяем все неоплаченные счета
        for (Invoice invoice : order.getInvoices()) {
            if (invoice.getStatus() == InvoiceStatus.PENDING) {
                invoice.setStatus(InvoiceStatus.CANCELLED);
            }
        }

        // 4. Сохраняем
        SalesOrder cancelled = salesOrderRepository.save(order);

        log.info(messageService.get("sales.order.processor.cancel.complete",
                order.getOrderNumber(), cancelledBy));

        return cancelled;
    }

    // =========================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =========================================================================

    /**
     * Конвертирует CartItem в CartItemDto
     */
    private List<CartItemDto> convertCartItemsToDto(List<CartItem> cartItems) {
        return cartItems.stream()
                .map(item -> CartItemDto.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .sku(item.getSku())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .category(item.getCategory())
                        .totalPrice(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Добавляет позиции в заказ
     */
    private void addOrderItems(SalesOrder order, List<CartItem> cartItems) {
        for (CartItem cartItem : cartItems) {
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
    }

    /**
     * Определяет тип заказа (розничный или оптовый)
     */
    private SalesOrderType determineOrderType(Long userId) {
        // TODO: Реализовать логику определения типа пользователя
        // Например, проверить UserTypeAssignment через UserTypeAssignmentService
        return SalesOrderType.RETAIL;
    }
}