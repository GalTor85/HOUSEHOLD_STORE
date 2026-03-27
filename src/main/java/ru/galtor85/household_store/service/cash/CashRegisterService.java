package ru.galtor85.household_store.service.cash;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.cash.CashRegisterNotFoundException;
import ru.galtor85.household_store.converter.CashRegisterConverter;
import ru.galtor85.household_store.dto.request.finance.CashRegisterCreateRequest;
import ru.galtor85.household_store.dto.response.finance.CashRegisterDto;
import ru.galtor85.household_store.dto.response.finance.CashRegisterSummaryDto;
import ru.galtor85.household_store.dto.request.finance.CashRegisterUpdateRequest;
import ru.galtor85.household_store.entity.finance.CashRegister;
import ru.galtor85.household_store.processor.cash.CashRegisterProcessor;
import ru.galtor85.household_store.repository.cash.CashRegisterRepository;
import ru.galtor85.household_store.repository.cash.CashTransactionRepository;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.validator.cash.CashRegisterValidator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashRegisterService {

    private final CashRegisterRepository cashRegisterRepository;
    private final CashTransactionRepository cashTransactionRepository;
    private final CashRegisterValidator validator;
    private final CashRegisterProcessor processor;
    private final CashRegisterConverter converter;
    private final MessageService messageService;

    // =========================================================================
    // ПУБЛИЧНЫЕ МЕТОДЫ ДЛЯ ДРУГИХ СЕРВИСОВ
    // =========================================================================

    /**
     * Проверяет существование кассы и возвращает её
     */
    public CashRegister validateCashRegisterExists(Long cashRegisterId) {
        return validator.validateExists(cashRegisterId);
    }

    /**
     * Проверяет, активна ли касса
     */
    public void validateCashRegisterActive(Long cashRegisterId) {
        CashRegister cashRegister = validator.validateExists(cashRegisterId);
        validator.validateActive(cashRegister);
    }

    /**
     * Получает текущий баланс кассы (для других сервисов)
     */
    public BigDecimal getCurrentBalance(Long cashRegisterId) {
        CashRegister cashRegister = validator.validateExists(cashRegisterId);
        return getCurrentBalance(cashRegister);
    }

    // =========================================================================
    // СОЗДАНИЕ КАССЫ
    // =========================================================================

    @Transactional
    public CashRegisterDto createCashRegister(CashRegisterCreateRequest request, Long createdBy) {
        log.info(messageService.get("cash.register.service.create.start", request.getRegisterNumber()));

        validator.validateRegisterNumberUnique(request.getRegisterNumber());
        validator.validateOpeningBalance(request.getOpeningBalance());

        CashRegister saved = processor.createCashRegister(request, createdBy);
        CashRegisterDto result = converter.toDto(saved);

        log.info(messageService.get("cash.register.service.created", result.getRegisterNumber()));

        return result;
    }

    // =========================================================================
    // ОБНОВЛЕНИЕ КАССЫ
    // =========================================================================

    @Transactional
    public CashRegisterDto updateCashRegister(Long cashRegisterId, CashRegisterUpdateRequest request) {
        log.info(messageService.get("cash.register.service.update.start", cashRegisterId));

        CashRegister cashRegister = validator.validateExists(cashRegisterId);
        CashRegister updated = processor.updateCashRegister(cashRegister, request);
        CashRegisterDto result = converter.toDto(updated);

        log.info(messageService.get("cash.register.service.updated", result.getRegisterNumber()));

        return result;
    }

    // =========================================================================
    // ПОЛУЧЕНИЕ КАСС
    // =========================================================================

    @Transactional(readOnly = true)
    public List<CashRegisterDto> getAllCashRegisters() {
        return cashRegisterRepository.findAll().stream()
                .map(converter::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CashRegisterDto> getActiveCashRegisters() {
        return cashRegisterRepository.findByIsActiveTrue().stream()
                .map(converter::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CashRegisterDto getCashRegisterById(Long cashRegisterId) {
        CashRegister cashRegister = validator.validateExists(cashRegisterId);
        return converter.toDto(cashRegister);
    }

    @Transactional(readOnly = true)
    public CashRegisterDto getCashRegisterByNumber(String registerNumber) {
        CashRegister cashRegister = cashRegisterRepository.findByRegisterNumber(registerNumber)
                .orElseThrow(() -> new CashRegisterNotFoundException(registerNumber));
        return converter.toDto(cashRegister);
    }

    // =========================================================================
    // ОТКРЫТИЕ / ЗАКРЫТИЕ КАССЫ
    // =========================================================================

    @Transactional
    public CashRegisterDto openCashRegister(Long cashRegisterId, BigDecimal openingBalance, Long cashierId) {
        log.info(messageService.get("cash.register.service.open.start", cashRegisterId, cashierId));

        CashRegister cashRegister = validator.validateExists(cashRegisterId);
        validator.validateClosed(cashRegister);
        validator.validateOpeningBalance(openingBalance);

        CashRegister opened = processor.openCashRegister(cashRegister, openingBalance, cashierId);
        CashRegisterDto result = converter.toDto(opened);

        log.info(messageService.get("cash.register.service.opened", result.getRegisterNumber()));

        return result;
    }

    @Transactional
    public CashRegisterDto closeCashRegister(Long cashRegisterId, BigDecimal closingBalance, Long cashierId) {
        log.info(messageService.get("cash.register.service.close.start", cashRegisterId, cashierId));

        CashRegister cashRegister = validator.validateExists(cashRegisterId);
        validator.validateActive(cashRegister);
        validator.validateCashier(cashRegister, cashierId);
        validator.validateClosingBalance(closingBalance);

        CashRegister closed = processor.closeCashRegister(cashRegister, closingBalance, cashierId);
        CashRegisterDto result = converter.toDto(closed);

        log.info(messageService.get("cash.register.service.closed", result.getRegisterNumber()));

        return result;
    }

    // =========================================================================
    // БАЛАНС И СТАТИСТИКА (ВНУТРЕННИЕ МЕТОДЫ)
    // =========================================================================

    private BigDecimal getCurrentBalance(CashRegister cashRegister) {
        BigDecimal totalIncome = cashTransactionRepository.getTotalAmountByCashRegister(cashRegister.getId());
        if (totalIncome == null) {
            totalIncome = BigDecimal.ZERO;
        }

        BigDecimal totalExpense = cashTransactionRepository.getTotalExpenseByCashRegisterAndDateRange(
                cashRegister.getId(), cashRegister.getOpenedAt(), LocalDateTime.now());
        if (totalExpense == null) {
            totalExpense = BigDecimal.ZERO;
        }

        return cashRegister.getOpeningBalance()
                .add(totalIncome)
                .subtract(totalExpense);
    }

    @Transactional(readOnly = true)
    public CashRegisterSummaryDto getSummary(Long cashRegisterId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info(messageService.get("cash.register.service.summary.start", cashRegisterId, startDate, endDate));

        CashRegister cashRegister = validator.validateExists(cashRegisterId);

        BigDecimal openingBalance = cashRegister.getOpeningBalance();
        BigDecimal totalIncome = cashTransactionRepository.getTotalIncomeByCashRegisterAndDateRange(
                cashRegisterId, startDate, endDate);
        BigDecimal totalExpense = cashTransactionRepository.getTotalExpenseByCashRegisterAndDateRange(
                cashRegisterId, startDate, endDate);
        BigDecimal netTurnover = cashTransactionRepository.getNetTurnoverByCashRegisterAndDateRange(
                cashRegisterId, startDate, endDate);
        long transactionCount = cashTransactionRepository.findByCashRegisterIdAndDateRange(
                cashRegisterId, startDate, endDate).size();

        if (totalIncome == null) totalIncome = BigDecimal.ZERO;
        if (totalExpense == null) totalExpense = BigDecimal.ZERO;
        if (netTurnover == null) netTurnover = BigDecimal.ZERO;

        BigDecimal closingBalance = openingBalance
                .add(totalIncome)
                .subtract(totalExpense);

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

        log.info(messageService.get("cash.register.service.summary.complete",
                cashRegisterId, summary.getTransactionCount(), summary.getNetTurnover()));

        return summary;
    }

    // =========================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =========================================================================

    public boolean existsById(Long cashRegisterId) {
        return cashRegisterRepository.existsById(cashRegisterId);
    }

    public boolean isActive(Long cashRegisterId) {
        return cashRegisterRepository.findById(cashRegisterId)
                .map(CashRegister::getIsActive)
                .orElse(false);
    }
}