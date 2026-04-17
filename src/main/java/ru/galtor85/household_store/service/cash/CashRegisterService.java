package ru.galtor85.household_store.service.cash;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.converter.CashRegisterConverter;
import ru.galtor85.household_store.dto.request.finance.CashRegisterCreateRequest;
import ru.galtor85.household_store.dto.request.finance.CashRegisterUpdateRequest;
import ru.galtor85.household_store.dto.response.finance.CashRegisterDto;
import ru.galtor85.household_store.dto.response.finance.CashRegisterSummaryDto;
import ru.galtor85.household_store.entity.finance.CashRegister;
import ru.galtor85.household_store.processor.cash.CashRegisterProcessor;
import ru.galtor85.household_store.repository.cash.CashRegisterRepository;
import ru.galtor85.household_store.repository.cash.CashTransactionRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.validator.cash.CashRegisterValidator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing cash register operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CashRegisterService {

    private static final String EXCESS_TYPE = "EXCESS";
    private static final String SHORTAGE_TYPE = "SHORTAGE";
    private static final String BALANCE_FORMAT = "%,.2f";
    private static final String DEFAULT_BALANCE = "0.00";

    private final CashRegisterRepository cashRegisterRepository;
    private final CashTransactionRepository cashTransactionRepository;
    private final CashRegisterValidator validator;
    private final CashRegisterProcessor processor;
    private final CashRegisterConverter converter;
    private final LogMessageService logMsg;
    private final MessageService messageService;


    // =========================================================================
    // PUBLIC METHODS FOR OTHER SERVICES
    // =========================================================================

    /**
     * Validates cash register existence and returns it.
     *
     * @param cashRegisterId cash register ID
     * @return CashRegister entity
     */
    public CashRegister validateCashRegisterExists(Long cashRegisterId) {
        return validator.validateExists(cashRegisterId);
    }

    /**
     * Gets current cash register balance for other services.
     *
     * @param cashRegisterId cash register ID
     * @return current balance
     */
    public BigDecimal getCurrentBalance(Long cashRegisterId) {
        CashRegister cashRegister = validator.validateExists(cashRegisterId);
        return cashRegister.getCurrentBalance();
    }

    // =========================================================================
    // CASH REGISTER CREATION
    // =========================================================================

    /**
     * Creates a new cash register.
     *
     * @param request creation request
     * @param createdBy ID of user creating the register
     * @return created cash register DTO
     */
    @Transactional
    public CashRegisterDto createCashRegister(CashRegisterCreateRequest request, Long createdBy) {
        log.info(logMsg.get("cash.register.service.create.start", request.getRegisterNumber()));

        validator.validateRegisterNumberUnique(request.getRegisterNumber());
        validator.validateOpeningBalance(request.getOpeningBalance());

        CashRegister saved = processor.createCashRegister(request, createdBy);
        CashRegisterDto result = converter.toSimpleDto(saved);

        log.info(logMsg.get("cash.register.service.created", result.getRegisterNumber()));

        return result;
    }

    // =========================================================================
    // CASH REGISTER UPDATE
    // =========================================================================

    /**
     * Updates an existing cash register.
     *
     * @param cashRegisterId cash register ID
     * @param request update request
     * @return updated cash register DTO
     */
    @Transactional
    public CashRegisterDto updateCashRegister(Long cashRegisterId, CashRegisterUpdateRequest request) {
        log.info(logMsg.get("cash.register.service.update.start", cashRegisterId));

        CashRegister cashRegister = validator.validateExists(cashRegisterId);
        CashRegister updated = processor.updateCashRegister(cashRegister, request);

        CashRegisterDto result = converter.toDto(updated, updated.getCurrentBalance());

        log.info(logMsg.get("cash.register.service.updated", result.getRegisterNumber()));

        return result;
    }

    /**
     * Deletes a cash register.
     *
     * @param cashRegisterId cash register ID
     */
    @Transactional
    public void deleteCashRegister(Long cashRegisterId) {
        log.info(logMsg.get("cash.register.service.delete.start", cashRegisterId));

        CashRegister cashRegister = validator.validateExists(cashRegisterId);

        // Check if cash register is active (opened)
        if (cashRegister.getIsActive()) {
            log.warn(logMsg.get("cash.register.delete.active", cashRegisterId));
            throw new IllegalStateException(
                    messageService.get("cash.register.delete.active", cashRegisterId)
            );
        }

        // Check if cash register has any transactions
        boolean hasTransactions = cashTransactionRepository.existsByCashRegisterId(cashRegisterId);
        if (hasTransactions) {
            log.warn(logMsg.get("cash.register.delete.has.transactions", cashRegisterId));
            throw new IllegalStateException(
                    messageService.get("cash.register.delete.has.transactions", cashRegisterId)
            );
        }

        cashRegisterRepository.delete(cashRegister);

        log.info(logMsg.get("cash.register.service.delete.complete", cashRegisterId));
    }

    // =========================================================================
    // CASH REGISTER RETRIEVAL
    // =========================================================================

    /**
     * Gets all cash registers.
     *
     * @return list of cash register DTOs
     */
    @Transactional(readOnly = true)
    public List<CashRegisterDto> getAllCashRegisters() {
        return cashRegisterRepository.findAll().stream()
                .map(cr -> converter.toDto(cr, cr.getCurrentBalance()))
                .collect(Collectors.toList());
    }

    /**
     * Gets all active cash registers.
     *
     * @return list of active cash register DTOs
     */
    @Transactional(readOnly = true)
    public List<CashRegisterDto> getActiveCashRegisters() {
        return cashRegisterRepository.findByIsActiveTrue().stream()
                .map(cr -> converter.toDto(cr, cr.getCurrentBalance()))
                .collect(Collectors.toList());
    }

    /**
     * Gets cash register by ID.
     *
     * @param cashRegisterId cash register ID
     * @return cash register DTO
     */
    @Transactional(readOnly = true)
    public CashRegisterDto getCashRegisterById(Long cashRegisterId) {
        CashRegister cashRegister = validator.validateExists(cashRegisterId);
        return converter.toDto(cashRegister, cashRegister.getCurrentBalance());
    }

    // =========================================================================
    // CASH REGISTER OPENING / CLOSING
    // =========================================================================

    /**
     * Opens a cash register for a new shift.
     *
     * @param cashRegisterId cash register ID
     * @param openingBalance opening balance
     * @param cashierId cashier ID
     * @return opened cash register DTO
     */
    @Transactional
    public CashRegisterDto openCashRegister(Long cashRegisterId, BigDecimal openingBalance, Long cashierId) {
        log.info(logMsg.get("cash.register.service.open.start", cashRegisterId, cashierId));

        CashRegister cashRegister = validator.validateExists(cashRegisterId);
        validator.validateClosed(cashRegister);
        validator.validateOpeningBalance(openingBalance);

        CashRegister opened = processor.openCashRegister(cashRegister, openingBalance, cashierId);
        CashRegisterDto result = converter.toDto(opened, opened.getCurrentBalance());

        log.info(logMsg.get("cash.register.service.opened", result.getRegisterNumber()));

        return result;
    }

    /**
     * Closes a cash register at the end of a shift.
     *
     * @param cashRegisterId cash register ID
     * @param closingBalance closing balance
     * @param discrepancyReason reason for discrepancy
     * @param cashierId cashier ID
     * @return closed cash register DTO
     */
    @Transactional
    public CashRegisterDto closeCashRegister(Long cashRegisterId, BigDecimal closingBalance,
                                             String discrepancyReason, Long cashierId) {
        log.info(logMsg.get("cash.register.service.close.start", cashRegisterId, cashierId));

        CashRegister cashRegister = validator.validateExists(cashRegisterId);
        validator.validateActive(cashRegister);
        validator.validateCashier(cashRegister, cashierId);
        validator.validateClosingBalance(closingBalance);

        BigDecimal calculatedBalance = cashRegister.getCurrentBalance();
        validator.validateDiscrepancyReason(closingBalance, calculatedBalance, discrepancyReason);

        BigDecimal discrepancy = closingBalance.subtract(calculatedBalance);

        if (discrepancy.compareTo(BigDecimal.ZERO) != 0) {
            log.warn(logMsg.get("cash.register.discrepancy.warning",
                    cashRegisterId,
                    formatBalance(calculatedBalance),
                    formatBalance(closingBalance),
                    formatBalance(discrepancy)));

            saveDiscrepancy(cashRegisterId, discrepancy, discrepancyReason, cashierId);
            log.info(logMsg.get("cash.register.discrepancy.saved", discrepancyReason));
        }

        CashRegister closed = processor.closeCashRegister(cashRegister, closingBalance);
        CashRegisterDto result = converter.toDto(closed, calculatedBalance);

        log.info(logMsg.get("cash.register.service.closed", result.getRegisterNumber()));

        return result;
    }

    // =========================================================================
    // BALANCE AND STATISTICS
    // =========================================================================

    /**
     * Gets cash register summary for a period.
     *
     * @param cashRegisterId cash register ID
     * @param startDate period start
     * @param endDate period end
     * @return summary DTO
     */
    @Transactional(readOnly = true)
    public CashRegisterSummaryDto getSummary(Long cashRegisterId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info(logMsg.get("cash.register.service.summary.start", cashRegisterId, startDate, endDate));

        CashRegister cashRegister = validator.validateExists(cashRegisterId);

        BigDecimal openingBalance = cashRegister.getOpeningBalance();
        BigDecimal totalIncome = nullToZero(cashTransactionRepository
                .getTotalIncomeByCashRegisterAndDateRange(cashRegisterId, startDate, endDate));
        BigDecimal totalExpense = nullToZero(cashTransactionRepository
                .getTotalExpenseByCashRegisterAndDateRange(cashRegisterId, startDate, endDate));
        BigDecimal netTurnover = nullToZero(cashTransactionRepository
                .getNetTurnoverByCashRegisterAndDateRange(cashRegisterId, startDate, endDate));
        long transactionCount = cashTransactionRepository
                .findByCashRegisterIdAndDateRange(cashRegisterId, startDate, endDate).size();

        BigDecimal closingBalance = openingBalance.add(totalIncome).subtract(totalExpense);

        CashRegisterSummaryDto summary = CashRegisterSummaryDto.builder()
                .cashRegisterId(cashRegisterId)
                .cashRegisterName(cashRegister.getName())
                .startDate(startDate)
                .endDate(endDate)
                .openingBalance(openingBalance)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netTurnover(netTurnover)
                .closingBalance(closingBalance)
                .transactionCount((int) transactionCount)
                .build();

        log.info(logMsg.get("cash.register.service.summary.complete",
                cashRegisterId, summary.getTransactionCount(), summary.getNetTurnover()));

        return summary;
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private void saveDiscrepancy(Long cashRegisterId, BigDecimal discrepancy,
                                 String discrepancyReason, Long cashierId) {
        try {
            String discrepancyType = discrepancy.compareTo(BigDecimal.ZERO) > 0 ? EXCESS_TYPE : SHORTAGE_TYPE;
            log.info(logMsg.get("cash.register.discrepancy.saved.details",
                    cashRegisterId, cashierId, discrepancyType,
                    formatBalance(discrepancy.abs()), discrepancyReason));
        } catch (Exception e) {
            log.error(logMsg.get("cash.register.discrepancy.save.failed",
                    cashRegisterId, e.getMessage()), e);
        }
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String formatBalance(BigDecimal balance) {
        if (balance == null) {
            return DEFAULT_BALANCE;
        }
        return String.format(BALANCE_FORMAT, balance);
    }
}