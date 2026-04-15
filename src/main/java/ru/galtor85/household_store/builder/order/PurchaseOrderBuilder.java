package ru.galtor85.household_store.builder.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.common.PurchaseOrderItemCreateDto;
import ru.galtor85.household_store.dto.request.order.PurchaseOrderCreateRequest;
import ru.galtor85.household_store.entity.order.OrderPaymentStatus;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.entity.order.PurchaseOrder;
import ru.galtor85.household_store.entity.order.PurchaseOrderItem;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.util.generator.NumberGenerator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder for PurchaseOrder entities and items.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseOrderBuilder {

    private final NumberGenerator numberGenerator;

    /**
     * Builds a PurchaseOrder entity from the create request.
     *
     * @param request   purchase order creation request
     * @param managerId ID of the manager creating the order
     * @return PurchaseOrder entity
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
                .paymentStatus(OrderPaymentStatus.PENDING)
                .createdBy(managerId)
                .notes(request.getNotes())
                .build();
    }

    /**
     * Builds a single PurchaseOrderItem.
     *
     * @param order   the parent purchase order
     * @param itemDto item data from request
     * @param product the product entity
     * @param price   the price for this item
     * @return PurchaseOrderItem entity
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
     * Builds a list of PurchaseOrderItems.
     *
     * @param order    the parent purchase order
     * @param itemDtos list of item DTOs
     * @param products list of corresponding products
     * @param prices   list of corresponding prices
     * @return list of PurchaseOrderItem entities
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

    /**
     * Calculates the total amount for a list of order items.
     *
     * @param items list of purchase order items
     * @return total amount
     */
    public BigDecimal calculateTotalAmount(List<PurchaseOrderItem> items) {
        return items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}