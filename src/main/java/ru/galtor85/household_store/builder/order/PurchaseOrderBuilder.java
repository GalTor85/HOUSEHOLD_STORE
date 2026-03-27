package ru.galtor85.household_store.builder.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.request.order.PurchaseOrderCreateRequest;
import ru.galtor85.household_store.dto.common.PurchaseOrderItemCreateDto;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.entity.order.PurchaseOrder;
import ru.galtor85.household_store.entity.order.PurchaseOrderItem;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.util.generator.NumberGenerator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseOrderBuilder {

    private final NumberGenerator numberGenerator;

    // =========================================================================
    // СОЗДАНИЕ ЗАКАЗА
    // =========================================================================

    /**
     * Создает сущность заказа на закупку из запроса
     */
    public PurchaseOrder buildOrder(PurchaseOrderCreateRequest request, Long managerId) {
        return PurchaseOrder.builder()
                .orderNumber(numberGenerator.generatePurchaseOrderNumber())
                .supplierId(request.getSupplierId())
                .status(OrderStatus.PENDING)
                .expectedDelivery(request.getExpectedDelivery())
                .warehouseLocation(request.getWarehouseLocation())
                .invoiceNumber(request.getInvoiceNumber())
                .paymentDue(request.getPaymentDue())
                .paymentStatus("PENDING")
                .createdBy(managerId)
                .notes(request.getNotes())
                .build();
    }

    // =========================================================================
    // СОЗДАНИЕ ПОЗИЦИЙ
    // =========================================================================

    /**
     * Создает одну позицию заказа
     */
    public PurchaseOrderItem buildOrderItem(PurchaseOrder order,
                                            PurchaseOrderItemCreateDto itemDto,
                                            Product product,
                                            BigDecimal price) {
        return PurchaseOrderItem.builder()
                .purchaseOrder(order)
                .productId(product.getId())
                .quantity(itemDto.getQuantity())
                .price(price)
                .supplierPrice(product.getSupplierPrice())
                .supplierSku(product.getSupplierSku())
                .productName(product.getName())
                .productSku(product.getSku())
                .receivedQuantity(0)
                .build();
    }

    /**
     * Создает список позиций заказа
     */
    public List<PurchaseOrderItem> buildOrderItems(PurchaseOrder order,
                                                   List<PurchaseOrderItemCreateDto> itemDtos,
                                                   List<Product> products,
                                                   List<BigDecimal> prices) {
        List<PurchaseOrderItem> items = new ArrayList<>();

        for (int i = 0; i < itemDtos.size(); i++) {
            PurchaseOrderItem item = buildOrderItem(
                    order,
                    itemDtos.get(i),
                    products.get(i),
                    prices.get(i)
            );
            items.add(item);
        }

        return items;
    }

    // =========================================================================
    // РАСЧЕТ СУММ
    // =========================================================================

    /**
     * Рассчитывает общую сумму заказа
     */
    public BigDecimal calculateTotalAmount(List<PurchaseOrderItem> items) {
        return items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}