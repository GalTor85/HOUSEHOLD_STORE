package ru.galtor85.household_store.mapper;

import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.CashTransactionRequest;
import ru.galtor85.household_store.entity.CashRegister;
import ru.galtor85.household_store.entity.CashTransaction;
import ru.galtor85.household_store.entity.Invoice;
import ru.galtor85.household_store.entity.TransactionType;

@Component
public class CashTransactionMapper {

    /**
     * Преобразует запрос в сущность
     */
    public CashTransaction toEntity(CashTransactionRequest request,
                                    CashRegister cashRegister,
                                    Invoice invoice,
                                    Long cashierId) {
        if (request == null) {
            return null;
        }

        return CashTransaction.builder()
                .cashRegister(cashRegister)
                .invoice(invoice)
                .transactionType(request.getTransactionType())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "RUB")
                .paymentMethod(request.getPaymentMethod())
                .cashierId(cashierId)
                .description(request.getDescription())
                .notes(request.getNotes())
                .build();
    }

    /**
     * Создает копию операции
     */
    public CashTransaction copy(CashTransaction source) {
        if (source == null) {
            return null;
        }

        return CashTransaction.builder()
                .cashRegister(source.getCashRegister())
                .invoice(source.getInvoice())
                .transactionType(source.getTransactionType())
                .amount(source.getAmount())
                .currency(source.getCurrency())
                .paymentMethod(source.getPaymentMethod())
                .cashierId(source.getCashierId())
                .description(source.getDescription())
                .notes(source.getNotes())
                .build();
    }

    /**
     * Создает операцию возврата
     */
    public CashTransaction toRefundTransaction(CashTransaction original, String reason) {
        if (original == null) {
            return null;
        }

        return CashTransaction.builder()
                .cashRegister(original.getCashRegister())
                .invoice(original.getInvoice())
                .transactionType(TransactionType.REFUND)
                .amount(original.getAmount())
                .currency(original.getCurrency())
                .paymentMethod(original.getPaymentMethod())
                .cashierId(original.getCashierId())
                .description("Возврат: " + reason)
                .notes(original.getNotes() != null ?
                        original.getNotes() + "\nВозврат: " + reason : "Возврат: " + reason)
                .build();
    }
}