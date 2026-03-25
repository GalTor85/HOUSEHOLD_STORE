// CashTransactionProcessor.java
package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.CashRegisterClosedException;
import ru.galtor85.household_store.advice.exception.CashRegisterNotFoundException;
import ru.galtor85.household_store.advice.exception.InsufficientCashException;
import ru.galtor85.household_store.dto.CashTransactionRequest;
import ru.galtor85.household_store.entity.*;
import ru.galtor85.household_store.repository.CashRegisterRepository;
import ru.galtor85.household_store.repository.CashTransactionRepository;
import ru.galtor85.household_store.repository.InvoiceRepository;
import ru.galtor85.household_store.service.MessageService;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class CashTransactionProcessor {

    private final CashTransactionRepository transactionRepository;
    private final CashRegisterRepository cashRegisterRepository;
    private final InvoiceRepository invoiceRepository;
    private final MessageService messageService;
    private final CashTransactionRepository cashTransactionRepository;

    @Transactional
    public CashTransaction processTransaction(CashTransactionRequest request, Long cashierId) {

        log.info(messageService.get("transaction.processor.start",
                request.getTransactionType().getLocalizedName(messageService),
                request.getAmount()));

        // 1. Проверяем кассу
        CashRegister cashRegister = cashRegisterRepository.findById(request.getCashRegisterId())
                .orElseThrow(() -> new CashRegisterNotFoundException(request.getCashRegisterId()));

        if (!cashRegister.getIsActive()) {
            throw new CashRegisterClosedException(cashRegister.getId());
        }

        // 2. Проверяем баланс для расхода
        if (request.getTransactionType() == TransactionType.EXPENSE) {
            BigDecimal currentBalance = cashRegister.getCurrentBalance();
            if (currentBalance.compareTo(request.getAmount()) < 0) {
                throw new InsufficientCashException(currentBalance, request.getAmount());
            }
        }

        // 3. Создаем операцию
        CashTransaction transaction = CashTransaction.builder()
                .cashRegister(cashRegister)
                .invoice(request.getInvoiceId() != null ?
                        invoiceRepository.findById(request.getInvoiceId()).orElse(null) : null)
                .transactionType(request.getTransactionType())
                .amount(request.getAmount())
                .paymentMethod(determinePaymentMethod(request))
                .cashierId(cashierId)
                .description(request.getDescription())
                .build();

        CashTransaction saved = transactionRepository.save(transaction);

        log.info(messageService.get("transaction.processor.complete",
                saved.getId(),
                transaction.getTransactionType().getLocalizedName(messageService),
                saved.getAmount()));

        return saved;
    }

    //Создает кассовую операцию

    @Transactional
    public CashTransaction createTransaction(CashTransactionRequest request,
                                             CashRegister cashRegister,
                                             Invoice invoice,
                                             Long cashierId) {
        log.info(messageService.get("cash.transaction.processor.create.start",
                request.getTransactionType().getLocalizedName(messageService),
                request.getAmount()));

        CashTransaction transaction = CashTransaction.builder()
                .cashRegister(cashRegister)
                .invoice(invoice)
                .transactionType(request.getTransactionType())
                .amount(request.getAmount())
                .currency(request.getNormalizedCurrency())
                .paymentMethod(request.getPaymentMethod())
                .cashierId(cashierId)
                .description(request.getDescription())
                .notes(request.getNotes())
                .build();

        CashTransaction saved = cashTransactionRepository.save(transaction);

        log.info(messageService.get("cash.transaction.processor.created",
                saved.getId(), saved.getAmount()));

        return saved;
    }

    /**
     * Создает возвратную операцию
     */
    @Transactional
    public CashTransaction createRefundTransaction(CashTransaction original, String reason, Long cashierId) {
        log.info(messageService.get("cash.transaction.processor.refund.start",
                original.getId(), reason));

        CashTransaction refund = CashTransaction.builder()
                .cashRegister(original.getCashRegister())
                .invoice(original.getInvoice())
                .transactionType(TransactionType.REFUND)
                .amount(original.getAmount())
                .currency(original.getCurrency())
                .paymentMethod(original.getPaymentMethod())
                .cashierId(cashierId)
                .description("Возврат: " + reason)
                .notes(original.getNotes() != null ?
                        original.getNotes() + "\nВозврат: " + reason : "Возврат: " + reason)
                .originalTransactionId(original.getId())
                .build();

        CashTransaction saved = cashTransactionRepository.save(refund);

        log.info(messageService.get("cash.transaction.processor.refund.created",
                saved.getId(), original.getId()));

        return saved;
    }


    private PaymentMethod determinePaymentMethod(CashTransactionRequest request) {
        // Если есть invoice, берем способ оплаты из него
        if (request.getInvoiceId() != null) {
            return invoiceRepository.findById(request.getInvoiceId())
                    .map(Invoice::getPaymentMethod)
                    .orElse(PaymentMethod.CASH);
        }
        return PaymentMethod.CASH;
    }
}