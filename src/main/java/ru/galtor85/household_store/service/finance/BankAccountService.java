package ru.galtor85.household_store.service.finance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.finance.BankAccountNotFoundException;
import ru.galtor85.household_store.converter.BankAccountConverter;
import ru.galtor85.household_store.dto.request.finance.BankAccountCreateRequest;
import ru.galtor85.household_store.dto.request.finance.BankAccountTransactionRequest;
import ru.galtor85.household_store.dto.response.finance.BankAccountDto;
import ru.galtor85.household_store.entity.finance.BankAccount;
import ru.galtor85.household_store.repository.finance.BankAccountRepository;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.validator.finance.BankAccountValidator;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing bank accounts
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final BankAccountConverter converter;
    private final BankAccountValidator validator;
    private final MessageService messageService;

    // =========================================================================
    // CREATE
    // =========================================================================

    /**
     * Creates a new bank account
     *
     * @param request   creation request
     * @param createdBy ID of user creating the account
     * @return created bank account DTO
     */
    @Transactional
    public BankAccountDto createBankAccount(BankAccountCreateRequest request, Long createdBy) {
        log.info(messageService.get("bank.account.service.create.start",
                request.getAccountNumber(), request.getBankName()));

        validator.validateBankNameLength(request.getBankName());
        validator.validateNameLength(request.getName());
        validator.validateAccountNumberUnique(request.getAccountNumber());
        validator.validateInitialBalance(request.getInitialBalance());

        BankAccount account = converter.toEntity(request, createdBy);
        BankAccount saved = bankAccountRepository.save(account);

        log.info(messageService.get("bank.account.service.created",
                saved.getAccountNumber(), saved.getId()));

        return converter.toDto(saved);
    }

    // =========================================================================
    // GET
    // =========================================================================

    /**
     * Retrieves bank account by ID
     *
     * @param accountId account ID
     * @return bank account DTO
     */
    @Transactional(readOnly = true)
    public BankAccountDto getBankAccountById(Long accountId) {
        BankAccount account = validator.validateExists(accountId);
        return converter.toDto(account);
    }

    /**
     * Retrieves bank account by account number
     *
     * @param accountNumber account number
     * @return bank account DTO
     */
    @Transactional(readOnly = true)
    public BankAccountDto getBankAccountByNumber(String accountNumber) {
        BankAccount account = bankAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new BankAccountNotFoundException(accountNumber));
        return converter.toDto(account);
    }

    /**
     * Retrieves all bank accounts
     *
     * @return list of bank account DTOs
     */
    @Transactional(readOnly = true)
    public List<BankAccountDto> getAllBankAccounts() {
        return bankAccountRepository.findAll().stream()
                .map(converter::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves active bank accounts only
     *
     * @return list of active bank account DTOs
     */
    @Transactional(readOnly = true)
    public List<BankAccountDto> getActiveBankAccounts() {
        return bankAccountRepository.findByActiveTrue().stream()
                .map(converter::toDto)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // TRANSACTIONS
    // =========================================================================

    /**
     * Deposits money to bank account
     *
     * @param request   deposit request
     * @param userId    ID of user performing the transaction
     * @return updated bank account DTO
     */
    @Transactional
    public BankAccountDto deposit(BankAccountTransactionRequest request, Long userId) {
        log.info(messageService.get("bank.account.service.deposit.start",
                request.getAccountId(), request.getAmount()));

        BankAccount account = validator.validateExists(request.getAccountId());
        validator.validateActive(account);

        account.deposit(request.getAmount());
        BankAccount saved = bankAccountRepository.save(account);

        // TODO: Create transaction record
        // TODO: Link to invoice if referenceId provided

        log.info(messageService.get("bank.account.service.deposit.complete",
                saved.getId(), request.getAmount(), saved.getBalance()));

        return converter.toDto(saved);
    }

    /**
     * Withdraws money from bank account
     *
     * @param request   withdrawal request
     * @param userId    ID of user performing the transaction
     * @return updated bank account DTO
     */
    @Transactional
    public BankAccountDto withdraw(BankAccountTransactionRequest request, Long userId) {
        log.info(messageService.get("bank.account.service.withdraw.start",
                request.getAccountId(), request.getAmount()));

        BankAccount account = validator.validateExists(request.getAccountId());
        validator.validateActive(account);
        validator.validateSufficientFunds(account, request.getAmount());

        account.withdraw(request.getAmount());
        BankAccount saved = bankAccountRepository.save(account);

        // TODO: Create transaction record
        // TODO: Link to invoice if referenceId provided

        log.info(messageService.get("bank.account.service.withdraw.complete",
                saved.getId(), request.getAmount(), saved.getBalance()));

        return converter.toDto(saved);
    }

    /**
     * Transfers money between bank accounts
     *
     * @param fromAccountId source account ID
     * @param toAccountId   destination account ID
     * @param amount        transfer amount
     * @param description   transfer description
     * @param userId        ID of user performing the transfer
     */
    @Transactional
    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount,
                         String description, Long userId) {
        log.info(messageService.get("bank.account.service.transfer.start",
                fromAccountId, toAccountId, amount));

        BankAccount fromAccount = validator.validateExists(fromAccountId);
        BankAccount toAccount = validator.validateExists(toAccountId);

        validator.validateActive(fromAccount);
        validator.validateActive(toAccount);
        validator.validateSufficientFunds(fromAccount, amount);

        fromAccount.withdraw(amount);
        toAccount.deposit(amount);

        bankAccountRepository.save(fromAccount);
        bankAccountRepository.save(toAccount);

        // TODO: Create transaction records for both accounts

        log.info(messageService.get("bank.account.service.transfer.complete",
                fromAccountId, toAccountId, amount));
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    /**
     * Deactivates a bank account
     *
     * @param accountId account ID
     * @param userId    ID of user performing deactivation
     * @return updated bank account DTO
     */
    @Transactional
    public BankAccountDto deactivate(Long accountId, Long userId) {
        log.info(messageService.get("bank.account.service.deactivate.start", accountId));

        BankAccount account = validator.validateExists(accountId);
        account.setActive(false);
        BankAccount saved = bankAccountRepository.save(account);

        log.info(messageService.get("bank.account.service.deactivate.complete", accountId));

        return converter.toDto(saved);
    }

    /**
     * Activates a bank account
     *
     * @param accountId account ID
     * @param userId    ID of user performing activation
     * @return updated bank account DTO
     */
    @Transactional
    public BankAccountDto activate(Long accountId, Long userId) {
        log.info(messageService.get("bank.account.service.activate.start", accountId));

        BankAccount account = validator.validateExists(accountId);
        account.setActive(true);
        BankAccount saved = bankAccountRepository.save(account);

        log.info(messageService.get("bank.account.service.activate.complete", accountId));

        return converter.toDto(saved);
    }

    // =========================================================================
    // BALANCE
    // =========================================================================

    /**
     * Gets current balance of bank account
     *
     * @param accountId account ID
     * @return current balance
     */
    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long accountId) {
        BankAccount account = validator.validateExists(accountId);
        return account.getBalance();
    }
}