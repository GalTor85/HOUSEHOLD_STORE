package ru.galtor85.household_store.builder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.PurchaseOrderCreateRequest;
import ru.galtor85.household_store.dto.PurchaseOrderItemCreateDto;
import ru.galtor85.household_store.dto.PurchaseOrderItemDto;
import ru.galtor85.household_store.entity.*;
import ru.galtor85.household_store.util.NumberGenerator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseOrderBuilder {

    private final NumberGenerator numberGenerator;

    /**
     * Создает сущность заказа из запроса
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

    /**
     * Создает позицию заказа
     */
    public PurchaseOrderItem buildOrderItem(PurchaseOrder order,
                                            PurchaseOrderItemDto itemDto,
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
     * Создает список позиций заказа из Create DTO
     */
    public List<PurchaseOrderItem> buildOrderItemsFromCreate(PurchaseOrder order,
                                                             List<PurchaseOrderItemCreateDto> itemDtos,
                                                             List<Product> products,
                                                             List<BigDecimal> prices) {
        List<PurchaseOrderItem> items = new ArrayList<>();

        for (int i = 0; i < itemDtos.size(); i++) {
            PurchaseOrderItemCreateDto itemDto = itemDtos.get(i);
            Product product = products.get(i);
            BigDecimal price = prices.get(i);

            PurchaseOrderItem item = buildOrderItem(order, itemDto, product, price);
            items.add(item);
        }

        return items;
    }

    /**
     * Создает одну позицию заказа из Create DTO
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
     * Рассчитывает общую сумму заказа
     */
    public BigDecimal calculateTotalAmount(List<PurchaseOrderItem> items) {
        return items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}