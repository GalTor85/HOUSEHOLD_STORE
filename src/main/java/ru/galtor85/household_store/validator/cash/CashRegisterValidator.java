package ru.galtor85.household_store.validator.cash;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.cash.CashRegisterClosedException;
import ru.galtor85.household_store.advice.exception.cash.CashRegisterNotFoundException;
import ru.galtor85.household_store.entity.finance.CashRegister;
import ru.galtor85.household_store.repository.cash.CashRegisterRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class CashRegisterValidator {

    private final CashRegisterRepository cashRegisterRepository;
    private final MessageService messageService;

    /**
     * Проверяет существование кассы
     */
    public CashRegister validateExists(Long cashRegisterId) {
        return cashRegisterRepository.findById(cashRegisterId)
                .orElseThrow(() -> {
                    log.error(messageService.get("cash.register.not.found.id", cashRegisterId));
                    return new CashRegisterNotFoundException(cashRegisterId);
                });
    }

    /**
     * Проверяет, что касса активна
     */
    public void validateActive(CashRegister cashRegister) {
        if (!cashRegister.getIsActive()) {
            log.error(messageService.get("cash.register.closed", cashRegister.getId()));
            throw new CashRegisterClosedException(cashRegister.getId());
        }
    }

    /**
     * Проверяет, что касса закрыта
     */
    public void validateClosed(CashRegister cashRegister) {
        if (cashRegister.getIsActive()) {
            log.error(messageService.get("cash.register.is.open", cashRegister.getId()));
            throw new IllegalStateException(
                    messageService.get("cash.register.is.open", cashRegister.getId())
            );
        }
    }

    /**
     * Проверяет уникальность номера кассы
     */
    public void validateRegisterNumberUnique(String registerNumber) {
        if (cashRegisterRepository.findByRegisterNumber(registerNumber).isPresent()) {
            log.error(messageService.get("cash.register.number.exists", registerNumber));
            throw new IllegalArgumentException(
                    messageService.get("cash.register.number.exists", registerNumber)
            );
        }
    }

    /**
     * Проверяет, что кассир тот же, кто открывал кассу
     */
    public void validateCashier(CashRegister cashRegister, Long cashierId) {
        if (cashRegister.getCashierId() != null && !cashRegister.getCashierId().equals(cashierId)) {
            log.error(messageService.get("cash.register.wrong.cashier",
                    cashRegister.getId(), cashierId));
            throw new IllegalStateException(
                    messageService.get("cash.register.wrong.cashier",
                            cashRegister.getId(), cashierId)
            );
        }
    }

    /**
     * Проверяет сумму открытия кассы
     */
    public void validateOpeningBalance(BigDecimal openingBalance) {
        if (openingBalance != null && openingBalance.compareTo(BigDecimal.ZERO) < 0) {
            log.error(messageService.get("cash.register.opening.balance.negative", openingBalance));
            throw new IllegalArgumentException(
                    messageService.get("cash.register.opening.balance.negative", openingBalance)
            );
        }
    }

    /**
     * Проверяет сумму закрытия кассы
     */
    public void validateClosingBalance(BigDecimal closingBalance) {
        if (closingBalance == null) {
            log.error(messageService.get("cash.register.closing.balance.required"));
            throw new IllegalArgumentException(
                    messageService.get("cash.register.closing.balance.required"));
        }

        if (closingBalance.compareTo(BigDecimal.ZERO) < 0) {
            log.error(messageService.get("cash.register.closing.balance.negative", closingBalance));
            throw new IllegalArgumentException(
                    messageService.get("cash.register.closing.balance.negative", closingBalance)
            );
        }
    }

    public void validateDiscrepancyReason(BigDecimal actualBalance,
                                          BigDecimal calculatedBalance,
                                          String discrepancyReason) {
        if (actualBalance == null || calculatedBalance == null) {
            return;
        }

        BigDecimal discrepancy = actualBalance.subtract(calculatedBalance);
        if (discrepancy.compareTo(BigDecimal.ZERO) != 0) {
            if (discrepancyReason == null || discrepancyReason.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        messageService.get("cash.register.discrepancy.reason.required"));
            }
        }
    }
}