package ru.galtor85.household_store.processor.cash;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.dto.request.finance.CashRegisterCreateRequest;
import ru.galtor85.household_store.dto.request.finance.CashRegisterUpdateRequest;
import ru.galtor85.household_store.entity.finance.CashRegister;
import ru.galtor85.household_store.mapper.finance.CashRegisterMapper;
import ru.galtor85.household_store.repository.cash.CashRegisterRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CashRegisterProcessor {

    private final CashRegisterRepository cashRegisterRepository;
    private final CashRegisterMapper mapper;
    private final MessageService messageService;

    // =========================================================================
    // СОЗДАНИЕ
    // =========================================================================

    /**
     * Создает новую кассу
     */
    @Transactional
    public CashRegister createCashRegister(CashRegisterCreateRequest request, Long createdBy) {
        log.info(messageService.get("cash.register.processor.create.start", request.getRegisterNumber()));

        CashRegister cashRegister = mapper.toEntity(request, createdBy);
        CashRegister saved = cashRegisterRepository.save(cashRegister);

        log.info(messageService.get("cash.register.processor.created", saved.getRegisterNumber()));

        return saved;
    }

    // =========================================================================
    // ОБНОВЛЕНИЕ
    // =========================================================================

    /**
     * Обновляет информацию о кассе
     */
    @Transactional
    public CashRegister updateCashRegister(CashRegister cashRegister, CashRegisterUpdateRequest request) {
        log.info(messageService.get("cash.register.processor.update.start", cashRegister.getId()));

        mapper.updateEntity(cashRegister, request);
        CashRegister updated = cashRegisterRepository.save(cashRegister);

        log.info(messageService.get("cash.register.processor.updated", updated.getRegisterNumber()));

        return updated;
    }

    // =========================================================================
    // ОТКРЫТИЕ КАССЫ
    // =========================================================================

    /**
     * Открывает кассу
     */
    @Transactional
    public CashRegister openCashRegister(CashRegister cashRegister, BigDecimal openingBalance, Long cashierId) {
        log.info(messageService.get("cash.register.processor.open.start", cashRegister.getId()));

        cashRegister.setIsActive(true);
        cashRegister.setOpeningBalance(openingBalance != null ? openingBalance : BigDecimal.ZERO);
        cashRegister.setOpenedAt(LocalDateTime.now());
        cashRegister.setClosingBalance(null);
        cashRegister.setClosedAt(null);
        cashRegister.setCashierId(cashierId);

        CashRegister opened = cashRegisterRepository.save(cashRegister);

        log.info(messageService.get("cash.register.processor.opened", opened.getRegisterNumber()));

        return opened;
    }

    // =========================================================================
    // ЗАКРЫТИЕ КАССЫ
    // =========================================================================

    /**
     * Закрывает кассу
     */
    @Transactional
    public CashRegister closeCashRegister(CashRegister cashRegister, BigDecimal closingBalance, Long cashierId) {
        log.info(messageService.get("cash.register.processor.close.start", cashRegister.getId()));

        BigDecimal currentBalance = cashRegister.getCurrentBalance();
        BigDecimal finalClosingBalance = closingBalance != null ? closingBalance : currentBalance;

        cashRegister.setIsActive(false);
        cashRegister.setClosingBalance(finalClosingBalance);
        cashRegister.setClosedAt(LocalDateTime.now());

        CashRegister closed = cashRegisterRepository.save(cashRegister);

        log.info(messageService.get("cash.register.processor.closed", closed.getRegisterNumber()));

        return closed;
    }

    // =========================================================================
    // ДОПОЛНИТЕЛЬНЫЕ ОПЕРАЦИИ
    // =========================================================================

    /**
     * Обновляет начальный баланс кассы (при открытии)
     */
    @Transactional
    public CashRegister updateOpeningBalance(CashRegister cashRegister, BigDecimal openingBalance) {
        log.info(messageService.get("cash.register.processor.update.balance.start",
                cashRegister.getId(), openingBalance));

        cashRegister.setOpeningBalance(openingBalance);
        cashRegister.setUpdatedAt(LocalDateTime.now());

        CashRegister updated = cashRegisterRepository.save(cashRegister);

        log.info(messageService.get("cash.register.processor.update.balance.complete",
                updated.getRegisterNumber()));

        return updated;
    }

    /**
     * Деактивирует кассу (принудительное закрытие без подсчета баланса)
     */
    @Transactional
    public CashRegister deactivateCashRegister(CashRegister cashRegister, String reason, Long deactivatedBy) {
        log.info(messageService.get("cash.register.processor.deactivate.start",
                cashRegister.getId(), reason));

        cashRegister.setIsActive(false);
        cashRegister.setClosedAt(LocalDateTime.now());
        cashRegister.setUpdatedAt(LocalDateTime.now());

        String deactivateNote = String.format("Деактивирована: %s (пользователь %d)", reason, deactivatedBy);
        // TODO: добавить поле deactivation_reason в сущность при необходимости

        CashRegister deactivated = cashRegisterRepository.save(cashRegister);

        log.info(messageService.get("cash.register.processor.deactivated",
                deactivated.getRegisterNumber()));

        return deactivated;
    }
}