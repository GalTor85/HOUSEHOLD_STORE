package ru.galtor85.household_store.builder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.PurchaseOrderCreateRequest;
import ru.galtor85.household_store.dto.PurchaseOrderItemDto;
import ru.galtor85.household_store.entity.*;
import ru.galtor85.household_store.util.EntityFinder;
import ru.galtor85.household_store.util.NumberGenerator;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class OrderBuilder {

    private final EntityFinder entityFinder;
    private final NumberGenerator numberGenerator;

    public Order buildPurchaseOrder(PurchaseOrderCreateRequest request, Long managerId) {
        return Order.builder()
                .orderNumber(numberGenerator.generatePurchaseOrderNumber())
                .supplierId(request.getSupplierId())
                .orderType(OrderType.PURCHASE)
                .status(OrderStatus.PENDING)
                .createdBy(managerId)
                .notes(request.getNotes())
                .build();
    }

    public OrderItem buildOrderItem(PurchaseOrderItemDto itemDto,
                                    SupplierProduct supplierProduct, Product product) {
        return OrderItem.builder()
                .productId(itemDto.getProductId())
                .supplierProductId(supplierProduct.getId())
                .quantity(itemDto.getQuantity())
                .price(supplierProduct.getSupplierPrice())
                .supplierPrice(supplierProduct.getSupplierPrice())
                .productName(product.getName())
                .productSku(product.getSku())
                .supplierSku(supplierProduct.getSupplierSku())
                .build();
    }

    public BigDecimal calculateAndAddItems(Order order, PurchaseOrderCreateRequest request) {
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (PurchaseOrderItemDto itemDto : request.getItems()) {
            Product product = entityFinder.findProductById(itemDto.getProductId());
            SupplierProduct supplierProduct = entityFinder.findSupplierProductBySupplierAndProduct(
                    request.getSupplierId(), itemDto.getProductId());

            BigDecimal itemTotal = supplierProduct.getSupplierPrice()
                    .multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            order.addItem(buildOrderItem(itemDto, supplierProduct, product));
        }

        return totalAmount;
    }
}