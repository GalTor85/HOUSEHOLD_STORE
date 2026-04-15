package ru.galtor85.household_store.processor.cash;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.dto.request.finance.CashTransactionRequest;
import ru.galtor85.household_store.entity.finance.*;
import ru.galtor85.household_store.repository.cash.CashTransactionRepository;
import ru.galtor85.household_store.service.cash.CashRegisterService;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;

/**
 * Processor for cash transaction operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CashTransactionProcessor {

    private final MessageService messageService;
    private final CashTransactionRepository cashTransactionRepository;
    private final CashRegisterService cashRegisterService;
    private final LogMessageService logMsg;

    /**
     * Creates a new cash transaction.
     *
     * @param request      the transaction request
     * @param cashRegister the cash register
     * @param invoice      the associated invoice (optional)
     * @param cashierId    ID of the cashier performing the transaction
     * @return created CashTransaction entity
     */
    @Transactional
    public CashTransaction createTransaction(CashTransactionRequest request,
                                             CashRegister cashRegister,
                                             Invoice invoice,
                                             Long cashierId) {
        log.info(logMsg.get("cash.transaction.processor.create.start",
                request.getTransactionType().getLocalizedName(messageService),
                request.getAmount()));

        // Get current balance BEFORE transaction
        BigDecimal currentBalance = cashRegisterService.getCurrentBalance(cashRegister.getId());

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
                .balanceBefore(currentBalance)
                .build();

        // Calculate balance after
        BigDecimal balanceAfter = currentBalance.add(
                request.getAmount().multiply(BigDecimal.valueOf(request.getTransactionType().getMultiplier()))
        );
        transaction.setBalanceAfter(balanceAfter);

        CashTransaction saved = cashTransactionRepository.save(transaction);

        log.info(logMsg.get("cash.transaction.processor.created",
                saved.getId(), saved.getAmount()));

        return saved;
    }

    /**
     * Creates a refund transaction for an existing transaction.
     *
     * @param original the original transaction to refund
     * @param reason   the reason for refund
     * @param cashierId ID of the cashier performing the refund
     * @return created refund CashTransaction entity
     */
    @Transactional
    public CashTransaction createRefundTransaction(CashTransaction original, String reason, Long cashierId) {
        log.info(logMsg.get("cash.transaction.processor.refund.start",
                original.getId(), reason));

        // Get current balance BEFORE refund
        BigDecimal currentBalance = cashRegisterService.getCurrentBalance(original.getCashRegister().getId());

        CashTransaction refund = CashTransaction.builder()
                .cashRegister(original.getCashRegister())
                .invoice(original.getInvoice())
                .transactionType(TransactionType.REFUND)
                .amount(original.getAmount())
                .currency(original.getCurrency())
                .paymentMethod(original.getPaymentMethod())
                .cashierId(cashierId)
                .description("Refund: " + reason)
                .notes(original.getNotes() != null ?
                        original.getNotes() + "\nRefund: " + reason : "Refund: " + reason)
                .originalTransactionId(original.getId())
                .balanceBefore(currentBalance)
                .build();

        // Calculate balance after
        BigDecimal balanceAfter = currentBalance.add(original.getAmount());
        refund.setBalanceAfter(balanceAfter);

        CashTransaction saved = cashTransactionRepository.save(refund);

        log.info(logMsg.get("cash.transaction.processor.refund.created",
                saved.getId(), original.getId()));

        return saved;
    }
}