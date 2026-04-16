package ru.galtor85.household_store.converter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.payment.PaymentTransactionDto;
import ru.galtor85.household_store.entity.payment.PaymentTransaction;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentTransactionConverter {

    private final MessageService messageService;

    /**
     * Converts PaymentTransaction entity to DTO
     *
     * @param transaction payment transaction entity
     * @return payment transaction DTO
     */
    public PaymentTransactionDto toDto(PaymentTransaction transaction) {
        if (transaction == null) {
            return null;
        }

        String localizedStatus = messageService.get(
                "payment.status." + transaction.getStatus().name()
        );

        return PaymentTransactionDto.builder()
                .id(transaction.getId())
                .paymentMethodId(transaction.getPaymentMethodId())
                .invoiceId(transaction.getInvoiceId())
                .orderId(transaction.getOrderId())
                .orderType(transaction.getOrderType() != null ?
                        transaction.getOrderType().name() : null)
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus())
                .localizedStatus(localizedStatus)
                .providerTransactionId(transaction.getProviderTransactionId())
                .providerPaymentUrl(transaction.getProviderPaymentUrl())
                .description(transaction.getDescription())
                .processingFee(transaction.getProcessingFee())
                .netAmount(transaction.getNetAmount())
                .completedAt(transaction.getCompletedAt())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    /**
     * Converts list of PaymentTransaction entities to DTOs
     *
     * @param transactions list of payment transaction entities
     * @return list of payment transaction DTOs
     */
    public List<PaymentTransactionDto> toDtoList(List<PaymentTransaction> transactions) {
        if (transactions == null) {
            return null;
        }
        return transactions.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

}