package ru.galtor85.household_store.converter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.order.PurchaseOrderDto;
import ru.galtor85.household_store.dto.response.order.PurchaseOrderItemDto;
import ru.galtor85.household_store.entity.order.PurchaseOrder;
import ru.galtor85.household_store.entity.order.PurchaseOrderItem;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseOrderConverter {

    private final MessageService messageService;

    /**
     * Конвертирует сущность заказа в DTO
     */
    public PurchaseOrderDto toDto(PurchaseOrder order, String supplierName) {
        if (order == null) {
            return null;
        }

        List<PurchaseOrderItemDto> itemDtos = toItemDtoList(order.getItems());

        String localizedStatus = messageService.get(
                "salesOrder.status." + order.getStatus().name()
        );

        return PurchaseOrderDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .supplierId(order.getSupplierId())
                .supplierName(supplierName)
                .status(order.getStatus())
                .localizedStatus(localizedStatus)
                .items(itemDtos)
                .expectedDelivery(order.getExpectedDelivery())
                .actualDelivery(order.getActualDelivery())
                .warehouseLocation(order.getWarehouseLocation())
                .receivedBy(order.getReceivedBy())
                .qualityCheck(order.getQualityCheck())
                .invoiceNumber(order.getInvoiceNumber())
                .paymentDue(order.getPaymentDue())
                .paymentStatus(order.getPaymentStatus())
                .subtotal(order.getSubtotal())
                .totalAmount(order.getTotalAmount())
                .createdBy(order.getCreatedBy())
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    /**
     * Конвертирует сущность позиции в DTO
     */
    public PurchaseOrderItemDto toItemDto(PurchaseOrderItem item) {
        if (item == null) {
            return null;
        }

        BigDecimal totalPrice = item.getPrice() != null ?
                item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())) :
                BigDecimal.ZERO;

        return PurchaseOrderItemDto.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .productSku(item.getProductSku())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .supplierPrice(item.getSupplierPrice())
                .supplierSku(item.getSupplierSku())
                .receivedQuantity(item.getReceivedQuantity())
                .remainingQuantity(item.getRemainingQuantity())
                .totalPrice(totalPrice)
                .build();
    }

    /**
     * Конвертирует список позиций в список DTO
     */
    public List<PurchaseOrderItemDto> toItemDtoList(List<PurchaseOrderItem> items) {
        if (items == null) {
            return null;
        }

        return items.stream()
                .map(this::toItemDto)
                .collect(Collectors.toList());
    }

    /**
     * Конвертирует сущность заказа в DTO без имени поставщика
     */
    public PurchaseOrderDto toDto(PurchaseOrder order) {
        return toDto(order, null);
    }

    /**
     * Конвертирует сущность заказа в DTO с дополнительной информацией
     */
    public PurchaseOrderDto toDtoWithDetails(PurchaseOrder order,
                                             String supplierName,
                                             int totalReceivedItems,
                                             BigDecimal totalReceivedAmount) {
        PurchaseOrderDto dto = toDto(order, supplierName);

        // Добавляем дополнительную информацию
        if (dto != null) {
            dto.setTotalReceivedItems(totalReceivedItems);
            dto.setTotalReceivedAmount(totalReceivedAmount);
        }

        return dto;
    }
}