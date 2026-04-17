package ru.galtor85.household_store.converter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.finance.CashTransactionDto;
import ru.galtor85.household_store.entity.finance.CashRegister;
import ru.galtor85.household_store.entity.finance.CashTransaction;
import ru.galtor85.household_store.entity.finance.Invoice;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.util.cash.CashBalanceCalculator;

import java.math.BigDecimal;

import static ru.galtor85.household_store.constants.TechnicalConstants.DEFAULT_CURRENCY_CODE;

/**
 * Converter for cash transaction entities to DTOs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CashTransactionConverter {

    private final MessageService messageService;
    private final CashBalanceCalculator balanceCalculator;

    /**
     * Converts cash transaction entity to DTO.
     *
     * @param transaction cash transaction entity
     * @return cash transaction DTO
     */
    public CashTransactionDto toDto(CashTransaction transaction) {
        if (transaction == null) {
            return null;
        }
        return buildBasicDto(transaction).build();
    }

    /**
     * Converts cash transaction entity to DTO with balance information.
     *
     * @param transaction cash transaction entity
     * @param balanceBefore balance before transaction
     * @param balanceAfter balance after transaction
     * @return cash transaction DTO with balance
     */
    public CashTransactionDto toDtoWithBalance(CashTransaction transaction,
                                               BigDecimal balanceBefore,
                                               BigDecimal balanceAfter) {
        if (transaction == null) {
            return null;
        }

        var type = transaction.getTransactionType();

        return CashTransactionDto.builder()
                .id(transaction.getId())
                .cashRegisterId(transaction.getCashRegister().getId())
                .transactionType(type)
                .localizedTransactionType(type != null ? type.getLocalizedName(messageService) : null)
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency() != null ? transaction.getCurrency() : DEFAULT_CURRENCY_CODE)
                .paymentMethod(transaction.getPaymentMethod())
                .localizedPaymentMethod(transaction.getPaymentMethod() != null ?
                        transaction.getPaymentMethod().getLocalizedName(messageService) : null)
                .description(transaction.getDescription())
                .notes(transaction.getNotes())
                .createdAt(transaction.getCreatedAt())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .sign(type != null ? type.getSign() : null)
                .color(type != null ? type.getColor() : null)
                .icon(type != null ? type.getIcon() : null)
                .build();
    }

    /**
     * Converts cash transaction entity to DTO with additional details.
     *
     * @param transaction cash transaction entity
     * @param cashRegister cash register entity
     * @param invoice invoice entity
     * @param cashier cashier user entity
     * @param balanceBefore balance before transaction
     * @return cash transaction DTO with full details
     */
    public CashTransactionDto toDtoWithDetails(CashTransaction transaction,
                                               CashRegister cashRegister,
                                               Invoice invoice,
                                               User cashier,
                                               BigDecimal balanceBefore) {
        if (transaction == null) {
            return null;
        }

        CashTransactionDto dto = toDto(transaction);
        if (dto != null) {
            dto.setCashRegisterId(cashRegister != null ? cashRegister.getId() : null);
            dto.setCashRegisterName(cashRegister != null ? cashRegister.getName() : null);
            dto.setCashRegisterNumber(cashRegister != null ? cashRegister.getRegisterNumber() : null);
            dto.setInvoiceId(invoice != null ? invoice.getId() : null);
            dto.setInvoiceNumber(invoice != null ? invoice.getInvoiceNumber() : null);
            dto.setCashierId(cashier != null ? cashier.getId() : null);
            dto.setCashierName(cashier != null ? cashier.getFirstName() + " " + cashier.getLastName() : null);
            dto.setCashierEmail(cashier != null ? cashier.getEmail() : null);
            dto.setBalanceBefore(balanceBefore);

            if (balanceBefore != null && transaction.getTransactionType() != null) {
                // Determine order type for correct multiplier calculation
                BigDecimal balanceAfter = balanceCalculator.getBalanceAfter(transaction, invoice, balanceBefore);
                dto.setBalanceAfter(balanceAfter);
            }
        }

        return dto;
    }


    private CashTransactionDto.CashTransactionDtoBuilder buildBasicDto(CashTransaction transaction) {
        var type = transaction.getTransactionType();

        return CashTransactionDto.builder()
                .id(transaction.getId())
                .transactionType(type)
                .localizedTransactionType(type != null ? type.getLocalizedName(messageService) : null)
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency() != null ? transaction.getCurrency() : DEFAULT_CURRENCY_CODE)
                .paymentMethod(transaction.getPaymentMethod())
                .localizedPaymentMethod(transaction.getPaymentMethod() != null ?
                        transaction.getPaymentMethod().getLocalizedName(messageService) : null)
                .description(transaction.getDescription())
                .notes(transaction.getNotes())
                .createdAt(transaction.getCreatedAt())
                .sign(type != null ? type.getSign() : null)
                .color(type != null ? type.getColor() : null)
                .icon(type != null ? type.getIcon() : null);
    }
}