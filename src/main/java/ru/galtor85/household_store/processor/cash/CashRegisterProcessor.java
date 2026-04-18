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
import ru.galtor85.household_store.service.i18n.LogMessageService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Processor for cash register operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CashRegisterProcessor {

    private final CashRegisterRepository cashRegisterRepository;
    private final CashRegisterMapper mapper;
    private final LogMessageService logMsg;

    // =========================================================================
    // CREATION
    // =========================================================================

    /**
     * Creates a new cash register.
     *
     * @param request   the creation request
     * @param createdBy ID of the user creating the register
     * @return created CashRegister entity
     */
    @Transactional
    public CashRegister createCashRegister(CashRegisterCreateRequest request, Long createdBy) {
        log.info(logMsg.get("cash.register.processor.create.start", request.getRegisterNumber()));

        CashRegister cashRegister = mapper.toEntity(request, createdBy);
        CashRegister saved = cashRegisterRepository.save(cashRegister);

        log.info(logMsg.get("cash.register.processor.created", saved.getRegisterNumber()));

        return saved;
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    /**
     * Updates an existing cash register.
     *
     * @param cashRegister the existing cash register entity
     * @param request      the update request
     * @return updated CashRegister entity
     */
    @Transactional
    public CashRegister updateCashRegister(CashRegister cashRegister, CashRegisterUpdateRequest request) {
        log.info(logMsg.get("cash.register.processor.update.start", cashRegister.getId()));

        mapper.updateEntity(cashRegister, request);
        CashRegister updated = cashRegisterRepository.save(cashRegister);

        log.info(logMsg.get("cash.register.processor.updated", updated.getRegisterNumber()));

        return updated;
    }

    // =========================================================================
    // OPENING
    // =========================================================================

    /**
     * Opens a cash register for a new shift.
     *
     * @param cashRegister   the cash register to open
     * @param openingBalance the opening balance (optional, defaults to 0)
     * @param cashierId      ID of the cashier opening the register
     * @return opened CashRegister entity
     */
    @Transactional
    public CashRegister openCashRegister(CashRegister cashRegister, BigDecimal openingBalance, Long cashierId) {
        log.info(logMsg.get("cash.register.processor.open.start", cashRegister.getId()));

        cashRegister.setIsActive(true);
        cashRegister.setOpeningBalance(openingBalance != null ? openingBalance : BigDecimal.ZERO);
        cashRegister.setOpenedAt(LocalDateTime.now());
        cashRegister.setClosingBalance(null);
        cashRegister.setClosedAt(null);
        cashRegister.setCashierId(cashierId);

        CashRegister opened = cashRegisterRepository.save(cashRegister);

        log.info(logMsg.get("cash.register.processor.opened", opened.getRegisterNumber()));

        return opened;
    }

    // =========================================================================
    // CLOSING
    // =========================================================================

    /**
     * Closes a cash register at the end of a shift.
     *
     * @param cashRegister   the cash register to close
     * @param closingBalance the closing balance (optional, defaults to current balance)
     * @return closed CashRegister entity
     */
    @Transactional
    public CashRegister closeCashRegister(CashRegister cashRegister, BigDecimal closingBalance) {
        log.info(logMsg.get("cash.register.processor.close.start", cashRegister.getId()));

        cashRegister.setIsActive(false);
        cashRegister.setClosingBalance(closingBalance);
        cashRegister.setClosedAt(LocalDateTime.now());

        CashRegister closed = cashRegisterRepository.save(cashRegister);

        log.info(logMsg.get("cash.register.processor.closed", closed.getRegisterNumber()));

        return closed;
    }
}