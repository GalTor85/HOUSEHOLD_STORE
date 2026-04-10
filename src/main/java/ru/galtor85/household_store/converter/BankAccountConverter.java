package ru.galtor85.household_store.converter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.config.FinancialConfig;
import ru.galtor85.household_store.dto.request.finance.BankAccountCreateRequest;
import ru.galtor85.household_store.dto.response.finance.BankAccountDto;
import ru.galtor85.household_store.entity.finance.BankAccount;
import ru.galtor85.household_store.service.currency.CurrencyCacheService;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Converter for BankAccount entity to/from DTO
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BankAccountConverter {

    private final MessageService messageService;
    private final CurrencyCacheService currencyService;
    private final FinancialConfig financialConfig;

    /**
     * Converts creation request to entity
     *
     * @param request   creation request
     * @param createdBy ID of user creating the account
     * @return bank account entity
     */
    public BankAccount toEntity(BankAccountCreateRequest request, Long createdBy) {
        if (request == null) {
            return null;
        }

        String defaultCurrency = financialConfig.getDefaultCurrency() != null ?
                financialConfig.getDefaultCurrency() : "RUB";


        return BankAccount.builder()
                .accountNumber(request.getAccountNumber())
                .name(request.getName())
                .bankName(request.getBankName())
                .bic(request.getBic())
                .correspondentAccount(request.getCorrespondentAccount())
                .iban(request.getIban())
                .swiftCode(request.getSwiftCode())
                .balance(request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO)
                .currency(request.getCurrency() != null ? request.getCurrency() : defaultCurrency)
                .active(request.getActive() != null ? request.getActive() : true)
                .accountType(request.getAccountType())
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Converts entity to DTO
     *
     * @param account bank account entity
     * @return bank account DTO
     */
    public BankAccountDto toDto(BankAccount account) {
        if (account == null) {
            return null;
        }

        BankAccountDto dto = BankAccountDto.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .name(account.getName())
                .bankName(account.getBankName())
                .bic(account.getBic())
                .correspondentAccount(account.getCorrespondentAccount())
                .iban(account.getIban())
                .swiftCode(account.getSwiftCode())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .accountType(account.getAccountType())
                .active(account.isActive())
                .createdBy(account.getCreatedBy())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();

        dto.initLocalizedFields(messageService, currencyService.getCurrencySymbol(account.getCurrency()));
        return dto;
    }
}