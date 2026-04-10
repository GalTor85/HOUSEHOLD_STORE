package ru.galtor85.household_store.service.cash;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.cash.InvoiceNotFoundException;
import ru.galtor85.household_store.converter.CashTransactionConverter;
import ru.galtor85.household_store.dto.response.finance.CashTransactionDto;
import ru.galtor85.household_store.dto.request.finance.CashTransactionRequest;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.entity.finance.*;
import ru.galtor85.household_store.mapper.finance.CashTransactionMapper;
import ru.galtor85.household_store.processor.cash.CashTransactionProcessor;
import ru.galtor85.household_store.repository.cash.CashTransactionRepository;
import ru.galtor85.household_store.repository.finance.InvoiceRepository;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.service.user.UserSearchService;
import ru.galtor85.household_store.validator.cash.CashTransactionValidator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static ru.galtor85.household_store.constants.PaginationConstants.DESC_SORT_DIRECTION;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashTransactionService {

    private final CashTransactionRepository cashTransactionRepository;
    private final InvoiceRepository invoiceRepository;
    private final CashTransactionValidator validator;
    private final CashTransactionProcessor processor;
    private final CashTransactionConverter converter;
    private final CashRegisterService cashRegisterService;
    private final UserSearchService userSearchService;
    private final MessageService messageService;

    // =========================================================================
    // СОЗДАНИЕ ОПЕРАЦИИ
    // =========================================================================

    /**
     * Создает кассовую операцию
     */
    @Transactional
    public CashTransactionDto createTransaction(CashTransactionRequest request, Long cashierId) {
        log.info(messageService.get("cash.transaction.service.create.start",
                request.getTransactionType(), request.getAmount()));

        // 1. Валидация запроса
        validator.validateRequest(request);

        // 2. Проверяем кассу
        CashRegister cashRegister = cashRegisterService.validateCashRegisterExists(request.getCashRegisterId());
        validator.validateCashRegisterActive(cashRegister);

        // 3. Проверяем счет (если указан)
        Invoice invoice = null;
        if (request.getInvoiceId() != null) {
            invoice = invoiceRepository.findById(request.getInvoiceId())
                    .orElseThrow(() -> new InvoiceNotFoundException(request.getInvoiceId()));
            validator.validateInvoiceForPayment(invoice, request.getAmount());
        }

        // 4. Проверяем баланс для расхода
        if (request.getTransactionType() == TransactionType.EXPENSE) {
            BigDecimal currentBalance = cashRegisterService.getCurrentBalance(cashRegister.getId());
            validator.validateSufficientBalance(cashRegister, request.getAmount(), currentBalance);
        }

        // 5. Создаем операцию через процессор
        CashTransaction transaction = processor.createTransaction(request, cashRegister, invoice, cashierId);

        // 6. Обновляем статус счета (если есть)
        if (invoice != null) {
            updateInvoiceStatus(invoice, request.getAmount());
        }

        // 7. Конвертируем в DTO
        CashTransactionDto result = converter.toDtoWithDetails(
                transaction, cashRegister, invoice,
                userSearchService.getUserById(cashierId),
                cashRegisterService.getCurrentBalance(cashRegister.getId())
        );

        log.info(messageService.get("cash.transaction.service.created",
                result.getId(), result.getAmount(), result.getTransactionType()));

        return result;
    }

    // =========================================================================
    // ПОЛУЧЕНИЕ ОПЕРАЦИЙ
    // =========================================================================

    /**
     * Получает операцию по ID
     */
    @Transactional(readOnly = true)
    public CashTransactionDto getTransactionById(Long transactionId) {
        CashTransaction transaction = validator.validateTransactionExists(transactionId);
        return enrichWithDetails(transaction);
    }

    /**
     * Получает все операции по кассе с пагинацией
     */
    @Transactional(readOnly = true)
    public Page<CashTransactionDto> getTransactionsByCashRegister(Long cashRegisterId,
                                                                  int page, int size,
                                                                  String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(DESC_SORT_DIRECTION) ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return cashTransactionRepository.findByCashRegisterId(cashRegisterId, pageable)
                .map(this::enrichWithDetails);
    }

    /**
     * Gets all transactions for an invoice with correct balance calculation
     * Balance is calculated based on ALL cash register transactions, not just this invoice
     *
     * @param invoiceId invoice identifier
     * @return list of transaction DTOs with correct sequential balances
     */
    @Transactional(readOnly = true)
    public List<CashTransactionDto> getTransactionsByInvoice(Long invoiceId) {
        List<CashTransaction> transactions = cashTransactionRepository
                .findByInvoiceIdOrdered(invoiceId);

        if (transactions.isEmpty()) {
            return Collections.emptyList();
        }

        List<CashTransactionDto> result = new ArrayList<>();

        for (CashTransaction tx : transactions) {
            // Just read saved balances - no calculation needed!
            CashTransactionDto dto = converter.toDtoWithBalance(
                    tx,
                    tx.getBalanceBefore(),  // ✅ already saved
                    tx.getBalanceAfter()    // ✅ already saved
            );
            enrichDtoWithDetails(dto, tx);
            result.add(dto);
        }

        return result;
    }

    /**
     * Получает операции за период
     */
    @Transactional(readOnly = true)
    public List<CashTransactionDto> getTransactionsByPeriod(LocalDateTime startDate,
                                                            LocalDateTime endDate,
                                                            Long cashRegisterId) {
        List<CashTransaction> transactions;
        if (cashRegisterId != null) {
            transactions = cashTransactionRepository.findByCashRegisterIdAndDateRange(
                    cashRegisterId, startDate, endDate);
        } else {
            transactions = cashTransactionRepository.findAll().stream()
                    .filter(t -> t.getCreatedAt().isAfter(startDate) &&
                            t.getCreatedAt().isBefore(endDate))
                    .collect(Collectors.toList());
        }

        return transactions.stream()
                .map(this::enrichWithDetails)
                .collect(Collectors.toList());
    }

    /**
     * Получает операции по типу
     */
    @Transactional(readOnly = true)
    public List<CashTransactionDto> getTransactionsByType(TransactionType type) {
        return cashTransactionRepository.findByTransactionType(type).stream()
                .map(this::enrichWithDetails)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // ОТМЕНА ОПЕРАЦИИ
    // =========================================================================

    /**
     * Отменяет кассовую операцию (создает возвратную операцию)
     */
    @Transactional
    public CashTransactionDto cancelTransaction(Long transactionId, String reason, Long cashierId) {
        log.info(messageService.get("cash.transaction.service.cancel.start", transactionId, reason));

        CashTransaction original = validator.validateTransactionExists(transactionId);
        validator.validateTransactionCancellable(original);

        // Проверяем кассу
        CashRegister cashRegister = cashRegisterService.validateCashRegisterExists(original.getCashRegister().getId());
        validator.validateCashRegisterActive(cashRegister);

        // Получаем баланс кассы ДО создания возврата
        BigDecimal balanceBeforeRefund = cashRegisterService.getCurrentBalance(cashRegister.getId());

        // Создаем возвратную операцию
        CashTransaction refundTransaction = processor.createRefundTransaction(original, reason, cashierId);

        // Рассчитываем баланс после возврата
        BigDecimal balanceAfterRefund = balanceBeforeRefund.add(refundTransaction.getAmount());

        // Обновляем статус счета (если есть)
        if (original.getInvoice() != null) {
            updateInvoiceStatusAfterRefund(original.getInvoice(), original.getAmount());
        }

        // ✅ Передаём правильные балансы в конвертер
        CashTransactionDto result = converter.toDtoWithBalance(
                refundTransaction, balanceBeforeRefund, balanceAfterRefund);

        enrichDtoWithDetails(result, refundTransaction);

        log.info(messageService.get("cash.transaction.service.cancelled",
                transactionId, refundTransaction.getId()));

        return result;
    }

    // =========================================================================
    // СТАТИСТИКА
    // =========================================================================

    /**
     * Получает сумму всех операций по кассе
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalAmountByCashRegister(Long cashRegisterId) {
        return cashTransactionRepository.getTotalAmountByCashRegister(cashRegisterId);
    }

    /**
     * Получает сумму приходов по кассе за период
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalIncomeByPeriod(Long cashRegisterId, LocalDateTime startDate, LocalDateTime
            endDate) {
        return cashTransactionRepository.getTotalIncomeByCashRegisterAndDateRange(
                cashRegisterId, startDate, endDate);
    }

    /**
     * Получает сумму расходов по кассе за период
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalExpenseByPeriod(Long cashRegisterId, LocalDateTime startDate, LocalDateTime
            endDate) {
        return cashTransactionRepository.getTotalExpenseByCashRegisterAndDateRange(
                cashRegisterId, startDate, endDate);
    }

    /**
     * Получает чистый оборот по кассе за период
     */
    @Transactional(readOnly = true)
    public BigDecimal getNetTurnoverByPeriod(Long cashRegisterId, LocalDateTime startDate, LocalDateTime
            endDate) {
        return cashTransactionRepository.getNetTurnoverByCashRegisterAndDateRange(
                cashRegisterId, startDate, endDate);
    }

    // =========================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =========================================================================

    /**
     * Обогащает DTO дополнительной информацией
     */
    private CashTransactionDto enrichWithDetails(CashTransaction transaction) {
        CashRegister cashRegister = transaction.getCashRegister();
        Invoice invoice = transaction.getInvoice();
        User cashier = transaction.getCashierId() != null ?
                userSearchService.getUserById(transaction.getCashierId()) : null;
        BigDecimal balanceBefore = cashRegisterService.getCurrentBalance(cashRegister.getId());

        return converter.toDtoWithDetails(transaction, cashRegister, invoice, cashier, balanceBefore);
    }

    /**
     * Обновляет статус счета после оплаты
     */
    private void updateInvoiceStatus(Invoice invoice, BigDecimal paidAmount) {
        BigDecimal totalPaid = cashTransactionRepository.findByInvoiceId(invoice.getId()).stream()
                .filter(t -> t.getTransactionType() == TransactionType.INCOME)
                .map(CashTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPaid.compareTo(invoice.getAmount()) >= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidDate(LocalDateTime.now());
        } else {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }
        invoiceRepository.save(invoice);
    }

    /**
     * Обновляет статус счета после возврата
     */
    private void updateInvoiceStatusAfterRefund(Invoice invoice, BigDecimal refundAmount) {
        BigDecimal totalPaid = cashTransactionRepository.findByInvoiceId(invoice.getId()).stream()
                .filter(t -> t.getTransactionType() == TransactionType.INCOME)
                .map(CashTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPaid.compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setStatus(InvoiceStatus.PENDING);
            invoice.setPaidDate(null);
        } else if (totalPaid.compareTo(invoice.getAmount()) < 0) {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        } else {
            invoice.setStatus(InvoiceStatus.PAID);
        }
        invoiceRepository.save(invoice);
    }

    private BigDecimal calculateBalanceBeforeDate(CashRegister cashRegister, LocalDateTime date) {
        // Суммируем все транзакции до указанной даты (включительно или нет — решите)
        List<CashTransaction> previous = cashTransactionRepository
                .findByCashRegisterIdAndDateRange(cashRegister.getId(), cashRegister.getOpenedAt(), date);
        BigDecimal netChange = previous.stream()
                .map(t -> t.getAmount().multiply(BigDecimal.valueOf(t.getTransactionType().getMultiplier())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return cashRegister.getOpeningBalance().add(netChange);
    }

    private void enrichDtoWithDetails(CashTransactionDto dto, CashTransaction tx) {
        CashRegister cr = tx.getCashRegister();
        Invoice inv = tx.getInvoice();
        User cashier = tx.getCashierId() != null ? userSearchService.getUserById(tx.getCashierId()) : null;

        dto.setCashRegisterId(cr.getId());
        dto.setCashRegisterName(cr.getName());
        dto.setCashRegisterNumber(cr.getRegisterNumber());

        if (inv != null) {
            dto.setInvoiceId(inv.getId());
            dto.setInvoiceNumber(inv.getInvoiceNumber());
        }

        if (cashier != null) {
            dto.setCashierId(cashier.getId());
            dto.setCashierName(cashier.getFirstName() + " " + cashier.getLastName());
            dto.setCashierEmail(cashier.getEmail());
        }

    }
}