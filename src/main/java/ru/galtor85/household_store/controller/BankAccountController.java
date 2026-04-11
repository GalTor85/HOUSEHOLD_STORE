package ru.galtor85.household_store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.galtor85.household_store.dto.request.finance.BankAccountCreateRequest;
import ru.galtor85.household_store.dto.request.finance.BankAccountTransactionRequest;
import ru.galtor85.household_store.dto.response.finance.BankAccountDto;
import ru.galtor85.household_store.dto.response.system.ApiResponse;
import ru.galtor85.household_store.service.finance.BankAccountService;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;
import java.util.List;

import static ru.galtor85.household_store.constants.EndpointConstants.CONTROL_BANK_ACCOUNTS;

/**
 * REST controller for managing bank accounts.
 *
 * <p>This controller provides endpoints for:</p>
 * <ul>
 *   <li>Creating new bank accounts</li>
 *   <li>Retrieving bank accounts (all, active, by ID, by number)</li>
 *   <li>Performing transactions (deposit, withdraw, transfer)</li>
 *   <li>Checking account balances</li>
 *   <li>Activating/deactivating accounts</li>
 * </ul>
 *
 * <p>All endpoints require ADMIN or MANAGER role for access.</p>
 *
 * @author G@LTor85
 * @since 1.0
 */
@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
@RestController
@RequestMapping(CONTROL_BANK_ACCOUNTS)
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Bank Account Management", description = "Endpoints for managing bank accounts and transfers")
public class BankAccountController extends BaseController {

    private final BankAccountService bankAccountService;
    private final MessageService messageService;


    // =========================================================================
    // CREATE BANK ACCOUNT
    // =========================================================================

    /**
     * Creates a new bank account.
     *
     * @param request the bank account creation request
     * @return created bank account DTO
     */
    @PostMapping
    @Operation(summary = "Create a new bank account",
            description = "Creates a new bank account with initial balance. Only accessible by ADMIN and MANAGER.")
    public ResponseEntity<ApiResponse<BankAccountDto>> createBankAccount(
            @Valid @RequestBody BankAccountCreateRequest request) {

        log.info(messageService.get("bank.account.controller.create.start", request.getAccountNumber()));

        Long userId = getCurrentUserId();
        BankAccountDto account = bankAccountService.createBankAccount(request, userId);

        log.info(messageService.get("bank.account.controller.create.success", account.getId(), account.getAccountNumber()));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("bank.account.created", account.getAccountNumber()),
                        account));
    }

    // =========================================================================
    // GET BANK ACCOUNTS
    // =========================================================================

    /**
     * Retrieves all bank accounts.
     *
     * @return list of all bank account DTOs
     */
    @GetMapping
    @Operation(summary = "Get all bank accounts",
            description = "Retrieves a list of all bank accounts. Only accessible by ADMIN and MANAGER.")
    public ResponseEntity<ApiResponse<List<BankAccountDto>>> getAllBankAccounts() {

        log.info(messageService.get("bank.account.controller.get.all.start"));

        List<BankAccountDto> accounts = bankAccountService.getAllBankAccounts();

        log.info(messageService.get("bank.account.controller.get.all.success", accounts.size()));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("bank.accounts.fetched"),
                accounts));
    }

    /**
     * Retrieves active bank accounts only.
     *
     * @return list of active bank account DTOs
     */
    @GetMapping("/active")
    @Operation(summary = "Get active bank accounts",
            description = "Retrieves a list of active bank accounts only. Only accessible by ADMIN and MANAGER.")
    public ResponseEntity<ApiResponse<List<BankAccountDto>>> getActiveBankAccounts() {

        log.info(messageService.get("bank.account.controller.get.active.start"));

        List<BankAccountDto> accounts = bankAccountService.getActiveBankAccounts();

        log.info(messageService.get("bank.account.controller.get.active.success", accounts.size()));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("bank.accounts.active.fetched"),
                accounts));
    }

    /**
     * Retrieves a bank account by its ID.
     *
     * @param accountId the bank account ID
     * @return bank account DTO
     */
    @GetMapping("/{accountId}")
    @Operation(summary = "Get bank account by ID",
            description = "Retrieves detailed information about a specific bank account.")
    public ResponseEntity<ApiResponse<BankAccountDto>> getBankAccountById(
            @Parameter(description = "Bank account ID", example = "1", required = true)
            @PathVariable Long accountId) {

        log.info(messageService.get("bank.account.controller.get.by.id.start", accountId));

        BankAccountDto account = bankAccountService.getBankAccountById(accountId);

        log.info(messageService.get("bank.account.controller.get.by.id.success", accountId));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("bank.account.fetched"),
                account));
    }

    /**
     * Retrieves a bank account by its account number.
     *
     * @param accountNumber the bank account number
     * @return bank account DTO
     */
    @GetMapping("/number/{accountNumber}")
    @Operation(summary = "Get bank account by account number",
            description = "Retrieves detailed information about a specific bank account by its number.")
    public ResponseEntity<ApiResponse<BankAccountDto>> getBankAccountByNumber(
            @Parameter(description = "Bank account number", example = "40702810123456789012", required = true)
            @PathVariable String accountNumber) {

        log.info(messageService.get("bank.account.controller.get.by.number.start", accountNumber));

        BankAccountDto account = bankAccountService.getBankAccountByNumber(accountNumber);

        log.info(messageService.get("bank.account.controller.get.by.number.success", accountNumber));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("bank.account.fetched"),
                account));
    }

    // =========================================================================
    // TRANSACTIONS
    // =========================================================================

    /**
     * Deposits money to a bank account.
     *
     * @param request the deposit request with account ID and amount
     * @return updated bank account DTO
     */
    @PostMapping("/deposit")
    @Operation(summary = "Deposit money to bank account",
            description = "Deposits specified amount to the bank account.")
    public ResponseEntity<ApiResponse<BankAccountDto>> deposit(
            @Valid @RequestBody BankAccountTransactionRequest request) {

        log.info(messageService.get("bank.account.controller.deposit.start",
                request.getAccountId(), request.getAmount()));

        Long userId = getCurrentUserId();
        BankAccountDto account = bankAccountService.deposit(request, userId);

        log.info(messageService.get("bank.account.controller.deposit.success",
                account.getId(), request.getAmount(), account.getBalance()));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("bank.account.deposit.success", request.getAmount()),
                account));
    }

    /**
     * Withdraws money from a bank account.
     *
     * @param request the withdrawal request with account ID and amount
     * @return updated bank account DTO
     */
    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw money from bank account",
            description = "Withdraws specified amount from the bank account. Validates sufficient funds.")
    public ResponseEntity<ApiResponse<BankAccountDto>> withdraw(
            @Valid @RequestBody BankAccountTransactionRequest request) {

        log.info(messageService.get("bank.account.controller.withdraw.start",
                request.getAccountId(), request.getAmount()));

        Long userId = getCurrentUserId();
        BankAccountDto account = bankAccountService.withdraw(request, userId);

        log.info(messageService.get("bank.account.controller.withdraw.success",
                account.getId(), request.getAmount(), account.getBalance()));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("bank.account.withdraw.success", request.getAmount()),
                account));
    }

    /**
     * Transfers money between two bank accounts.
     *
     * @param fromAccountId source account ID
     * @param toAccountId   destination account ID
     * @param amount        transfer amount
     * @param description   transfer description (optional)
     * @return success response
     */
    @PostMapping("/transfer")
    @Operation(summary = "Transfer money between bank accounts",
            description = "Transfers specified amount from one bank account to another.")
    public ResponseEntity<ApiResponse<Void>> transfer(
            @Parameter(description = "Source account ID", example = "1", required = true)
            @RequestParam Long fromAccountId,
            @Parameter(description = "Destination account ID", example = "2", required = true)
            @RequestParam Long toAccountId,
            @Parameter(description = "Transfer amount", example = "5000.00", required = true)
            @RequestParam BigDecimal amount,
            @Parameter(description = "Transfer description", example = "Fund transfer")
            @RequestParam(required = false) String description) {

        log.info(messageService.get("bank.account.controller.transfer.start",
                fromAccountId, toAccountId, amount));

        Long userId = getCurrentUserId();
        bankAccountService.transfer(fromAccountId, toAccountId, amount, description, userId);

        log.info(messageService.get("bank.account.controller.transfer.success",
                fromAccountId, toAccountId, amount));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("bank.account.transfer.success", amount),
                null));
    }

    // =========================================================================
    // BALANCE
    // =========================================================================

    /**
     * Gets the current balance of a bank account.
     *
     * @param accountId the bank account ID
     * @return current balance
     */
    @GetMapping("/{accountId}/balance")
    @Operation(summary = "Get bank account balance",
            description = "Returns the current balance of the specified bank account.")
    public ResponseEntity<ApiResponse<BigDecimal>> getBalance(
            @Parameter(description = "Bank account ID", example = "1", required = true)
            @PathVariable Long accountId) {

        log.info(messageService.get("bank.account.controller.balance.start", accountId));

        BigDecimal balance = bankAccountService.getBalance(accountId);

        log.info(messageService.get("bank.account.controller.balance.success", accountId, balance));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("bank.account.balance.fetched"),
                balance));
    }

    // =========================================================================
    // ACCOUNT STATUS
    // =========================================================================

    /**
     * Deactivates a bank account.
     *
     * @param accountId the bank account ID
     * @return updated bank account DTO
     */
    @PatchMapping("/{accountId}/deactivate")
    @Operation(summary = "Deactivate bank account",
            description = "Deactivates the specified bank account. Cannot be used for transactions when deactivated.")
    public ResponseEntity<ApiResponse<BankAccountDto>> deactivate(
            @Parameter(description = "Bank account ID", example = "1", required = true)
            @PathVariable Long accountId) {

        log.info(messageService.get("bank.account.controller.deactivate.start", accountId));

        Long userId = getCurrentUserId();
        BankAccountDto account = bankAccountService.deactivate(accountId, userId);

        log.info(messageService.get("bank.account.controller.deactivate.success", accountId));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("bank.account.deactivated"),
                account));
    }

    /**
     * Activates a bank account.
     *
     * @param accountId the bank account ID
     * @return updated bank account DTO
     */
    @PatchMapping("/{accountId}/activate")
    @Operation(summary = "Activate bank account",
            description = "Activates the specified bank account. Makes it available for transactions.")
    public ResponseEntity<ApiResponse<BankAccountDto>> activate(
            @Parameter(description = "Bank account ID", example = "1", required = true)
            @PathVariable Long accountId) {

        log.info(messageService.get("bank.account.controller.activate.start", accountId));

        Long userId = getCurrentUserId();
        BankAccountDto account = bankAccountService.activate(accountId, userId);

        log.info(messageService.get("bank.account.controller.activate.success", accountId));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("bank.account.activated"),
                account));
    }
}