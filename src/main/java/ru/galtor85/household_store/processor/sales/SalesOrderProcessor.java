package ru.galtor85.household_store.processor.sales;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.builder.order.SalesOrderBuilder;
import ru.galtor85.household_store.dto.request.order.SalesOrderCreateRequest;
import ru.galtor85.household_store.dto.request.price.PriceCalculationRequest;
import ru.galtor85.household_store.dto.response.cart.CartItemDto;
import ru.galtor85.household_store.dto.response.finance.PriceCalculationResult;
import ru.galtor85.household_store.dto.response.user.UserTypeAssignmentDto;
import ru.galtor85.household_store.entity.cart.Cart;
import ru.galtor85.household_store.entity.cart.CartItem;
import ru.galtor85.household_store.entity.finance.Invoice;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.entity.order.SalesOrderItem;
import ru.galtor85.household_store.entity.order.SalesOrderType;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.user.UserType;
import ru.galtor85.household_store.processor.invoice.InvoiceAutoCreationProcessor;
import ru.galtor85.household_store.processor.price.PriceCalculationProcessor;
import ru.galtor85.household_store.repository.order.SalesOrderRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.user.UserTypeAssignmentService;
import ru.galtor85.household_store.validator.order.SalesOrderValidator;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Processor for sales order operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SalesOrderProcessor {

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderBuilder builder;
    private final LogMessageService logMsg;
    private final InvoiceAutoCreationProcessor invoiceAutoCreationProcessor;
    private final SalesOrderValidator validator;
    private final UserTypeAssignmentService userTypeAssignmentService;
    private final PriceCalculationProcessor priceCalculationProcessor;

    // =========================================================================
    // CREATE ORDER FROM CART
    // =========================================================================

    /**
     * Creates a sales order from a shopping cart.
     *
     * @param cart            the shopping cart
     * @param shippingAddress the shipping address
     * @param userId          the user ID
     * @return created SalesOrder entity
     */
    @Transactional
    public SalesOrder createOrderFromCart(Cart cart, String shippingAddress, Long userId) {
        log.info(logMsg.get("sales.order.processor.create.from.cart.start", userId));

        validator.validateCartNotEmpty(cart);
        validator.validateCartItems(cart);

        List<CartItemDto> items = convertCartItemsToDto(cart.getItems());

        PriceCalculationRequest priceRequest = PriceCalculationRequest.builder()
                .userId(userId)
                .items(items)
                .shippingAddress(shippingAddress)
                .applyUserTypeDiscounts(true)
                .applyPromoCode(true)
                .applyPriceRules(true)
                .build();

        PriceCalculationResult priceResult = priceCalculationProcessor.calculatePrice(priceRequest);

        SalesOrderCreateRequest createRequest = SalesOrderCreateRequest.builder()
                .userId(userId)
                .orderType(determineOrderType(userId))
                .shippingAddress(shippingAddress)
                .build();

        SalesOrder savedOrder = createAndSaveOrder(createRequest, priceResult, userId, cart.getItems(), null);

        log.info(logMsg.get("sales.order.processor.create.from.cart.complete",
                savedOrder.getOrderNumber(), userId, priceResult.getFinalTotal()));

        return savedOrder;
    }

    // =========================================================================
    // DIRECT ORDER CREATION (BY MANAGER)
    // =========================================================================

    /**
     * Creates a sales order directly from a request.
     *
     * @param request  the creation request
     * @param products list of products
     * @param prices   list of prices
     * @param userId   the user ID
     * @return created SalesOrder entity
     */
    @Transactional
    public SalesOrder createSalesOrder(SalesOrderCreateRequest request,
                                       List<Product> products,
                                       List<BigDecimal> prices,
                                       Long userId) {
        log.info(logMsg.get("sales.order.processor.create.start", userId));

        validator.validateCreateRequest(request);
        validator.validateProducts(request.getItems());

        SalesOrder order = builder.buildOrder(request, userId);
        List<SalesOrderItem> items = builder.buildOrderItems(order, request.getItems(), products, prices);

        BigDecimal subtotal = builder.calculateTotalAmount(items);
        BigDecimal discountAmount = request.getEffectiveDiscountAmount();
        BigDecimal shippingAmount = request.getEffectiveShippingAmount();
        BigDecimal taxAmount = request.getEffectiveTaxAmount();
        BigDecimal totalAmount = subtotal.add(shippingAmount).add(taxAmount).subtract(discountAmount);

        PriceCalculationResult priceResult = PriceCalculationResult.builder()
                .originalTotal(subtotal)
                .finalTotal(totalAmount)
                .totalDiscount(discountAmount)
                .build();

        SalesOrder savedOrder = createAndSaveOrder(request, priceResult, userId, null, items);
        savedOrder.setShippingAmount(shippingAmount);
        savedOrder.setTaxAmount(taxAmount);

        log.info(logMsg.get("sales.order.processor.create.complete",
                savedOrder.getOrderNumber(), userId, items.size(), totalAmount));

        return savedOrder;
    }

    /**
     * Creates a sales order from cart with promo code.
     *
     * @param cart            the shopping cart
     * @param shippingAddress the shipping address
     * @param promoCode       the promo code
     * @param userId          the user ID
     * @return created SalesOrder entity
     */
    @Transactional
    public SalesOrder createOrderFromCartWithPromo(Cart cart, String shippingAddress,
                                                   String promoCode, Long userId) {
        log.info(logMsg.get("sales.order.processor.create.from.cart.promo.start", userId, promoCode));

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

        PriceCalculationResult priceResult = priceCalculationProcessor.calculatePrice(priceRequest);

        SalesOrderCreateRequest createRequest = SalesOrderCreateRequest.builder()
                .userId(userId)
                .orderType(determineOrderType(userId))
                .shippingAddress(shippingAddress)
                .build();

        return createAndSaveOrder(createRequest, priceResult, userId, cart.getItems(), null);
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

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
     * Determines the order type based on user's assigned type.
     *
     * @param userId the user ID
     * @return SalesOrderType (WHOLESALE for WHOLESALE/VIP/PARTNER, RETAIL otherwise)
     */
    private SalesOrderType determineOrderType(Long userId) {
        UserTypeAssignmentDto userTypeAssignment = userTypeAssignmentService.getCurrentUserType(userId);

        if (userTypeAssignment == null) {
            log.debug(logMsg.get("sales.order.type.default.retail", userId));
            return SalesOrderType.RETAIL;
        }

        UserType userType = userTypeAssignment.getUserType();

        SalesOrderType orderType = switch (userType) {
            case WHOLESALE, VIP, PARTNER, CORPORATE -> SalesOrderType.WHOLESALE;
            default -> SalesOrderType.RETAIL;
        };

        log.debug(logMsg.get("sales.order.type.determined", userId, userType, orderType));

        return orderType;
    }

    /**
     * Creates and saves a sales order with calculated prices.
     *
     * @param createRequest the order creation request
     * @param priceResult   the calculated price result
     * @param userId        the user ID
     * @param cartItems     the cart items (null for direct creation)
     * @param orderItems    pre-built order items (null for cart creation)
     * @return saved SalesOrder entity
     */
    private SalesOrder createAndSaveOrder(SalesOrderCreateRequest createRequest,
                                          PriceCalculationResult priceResult,
                                          Long userId,
                                          List<CartItem> cartItems,
                                          List<SalesOrderItem> orderItems) {

        SalesOrder order = builder.buildOrder(createRequest, userId);
        order.setStatus(OrderStatus.PENDING);
        order.setSubtotal(priceResult.getOriginalTotal());
        order.setDiscountAmount(priceResult.getTotalDiscount());
        order.setTotalAmount(priceResult.getFinalTotal());

        if (cartItems != null) {
            addOrderItems(order, cartItems);
        } else if (orderItems != null) {
            order.setItems(orderItems);
        }

        SalesOrder savedOrder = salesOrderRepository.save(order);

        Invoice invoice = invoiceAutoCreationProcessor.createInvoiceForOrder(order, userId);
        if (invoice != null) {
            savedOrder.addInvoice(invoice);
            salesOrderRepository.save(savedOrder);
            log.info(logMsg.get("sales.order.processor.invoice.created",
                    invoice.getInvoiceNumber(), savedOrder.getOrderNumber()));
        }

        return savedOrder;
    }
}