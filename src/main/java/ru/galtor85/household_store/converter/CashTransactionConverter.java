package ru.galtor85.household_store.converter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.CashTransactionDto;
import ru.galtor85.household_store.entity.CashRegister;
import ru.galtor85.household_store.entity.CashTransaction;
import ru.galtor85.household_store.entity.Invoice;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.service.MessageService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CashTransactionConverter {

    private final MessageService messageService;

    /**
     * Конвертирует сущность кассовой операции в DTO
     */
    public CashTransactionDto toDto(CashTransaction transaction) {
        if (transaction == null) {
            return null;
        }

        return buildBasicDto(transaction).build();
    }

    /**
     * Конвертирует сущность кассовой операции в DTO с дополнительной информацией
     */
    public CashTransactionDto toDto(CashTransaction transaction,
                                    CashRegister cashRegister,
                                    Invoice invoice,
                                    User cashier,
                                    BigDecimal balanceBefore) {
        if (transaction == null) {
            return null;
        }

        var builder = buildBasicDto(transaction)
                .cashRegisterId(cashRegister != null ? cashRegister.getId() : null)
                .cashRegisterName(cashRegister != null ? cashRegister.getName() : null)
                .cashRegisterNumber(cashRegister != null ? cashRegister.getRegisterNumber() : null)
                .invoiceId(invoice != null ? invoice.getId() : null)
                .invoiceNumber(invoice != null ? invoice.getInvoiceNumber() : null)
                .cashierId(cashier != null ? cashier.getId() : null)
                .cashierName(cashier != null ? cashier.getFirstName() + " " + cashier.getLastName() : null)
                .cashierEmail(cashier != null ? cashier.getEmail() : null)
                .balanceBefore(balanceBefore);

        // Рассчитываем баланс после операции
        if (balanceBefore != null && transaction.getTransactionType() != null) {
            BigDecimal balanceAfter = balanceBefore.add(
                    transaction.getAmount().multiply(
                            BigDecimal.valueOf(transaction.getTransactionType().getMultiplier())
                    )
            );
            builder.balanceAfter(balanceAfter);
        }

        return builder.build();
    }

    /**
     * Конвертирует сущность кассовой операции в DTO с информацией о балансе
     */
    public CashTransactionDto toDtoWithBalance(CashTransaction transaction,
                                               BigDecimal balanceBefore,
                                               BigDecimal balanceAfter) {
        if (transaction == null) {
            return null;
        }

        return buildBasicDto(transaction)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .build();
    }

    /**
     * Конвертирует список сущностей в список DTO
     */
    public List<CashTransactionDto> toDtoList(List<CashTransaction> transactions) {
        if (transactions == null) {
            return null;
        }
        return transactions.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Конвертирует список сущностей в список DTO с расчетом баланса
     */
    public List<CashTransactionDto> toDtoListWithBalance(List<CashTransaction> transactions,
                                                         BigDecimal openingBalance) {
        if (transactions == null) {
            return null;
        }

        BigDecimal currentBalance = openingBalance != null ? openingBalance : BigDecimal.ZERO;
        List<CashTransactionDto> result = new ArrayList<>();

        for (CashTransaction transaction : transactions) {
            BigDecimal balanceBefore = currentBalance;
            BigDecimal balanceAfter = currentBalance.add(
                    transaction.getAmount().multiply(
                            BigDecimal.valueOf(transaction.getTransactionType().getMultiplier())
                    )
            );

            result.add(toDtoWithBalance(transaction, balanceBefore, balanceAfter));
            currentBalance = balanceAfter;
        }

        return result;
    }

    /**
     * Создает базовый билдер с основными полями
     */
    private CashTransactionDto.CashTransactionDtoBuilder buildBasicDto(CashTransaction transaction) {
        var type = transaction.getTransactionType();

        return CashTransactionDto.builder()
                .id(transaction.getId())
                .transactionType(type)
                .localizedTransactionType(type != null ? type.getLocalizedName(messageService) : null)
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency() != null ? transaction.getCurrency() : "RUB")
                .paymentMethod(transaction.getPaymentMethod())
                .localizedPaymentMethod(transaction.getPaymentMethod() != null ?
                        transaction.getPaymentMethod().getLocalizedName(messageService) : null)
                .description(transaction.getDescription())
                .notes(transaction.getNotes())
                .createdAt(transaction.getCreatedAt())
                // UI поля
                .sign(type != null ? type.getSign() : null)
                .color(type != null ? type.getColor() : null)
                .icon(type != null ? type.getIcon() : null);
    }

    // =========================================================================
    // УПРОЩЕННЫЕ МЕТОДЫ ДЛЯ ОТЧЕТОВ
    // =========================================================================

    /**
     * Конвертирует сущность в упрощенный DTO (без деталей кассы и кассира)
     */
    public CashTransactionDto toSimpleDto(CashTransaction transaction) {
        if (transaction == null) {
            return null;
        }

        var type = transaction.getTransactionType();

        return CashTransactionDto.builder()
                .id(transaction.getId())
                .transactionType(type)
                .localizedTransactionType(type != null ? type.getLocalizedName(messageService) : null)
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency() != null ? transaction.getCurrency() : "RUB")
                .paymentMethod(transaction.getPaymentMethod())
                .localizedPaymentMethod(transaction.getPaymentMethod() != null ?
                        transaction.getPaymentMethod().getLocalizedName(messageService) : null)
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .sign(type != null ? type.getSign() : null)
                .build();
    }

    /**
     * Конвертирует сущность в DTO для отчета по кассиру
     */
    public CashTransactionDto toReportDto(CashTransaction transaction,
                                          String cashierName,
                                          BigDecimal balanceBefore,
                                          BigDecimal balanceAfter) {
        if (transaction == null) {
            return null;
        }

        var type = transaction.getTransactionType();

        return CashTransactionDto.builder()
                .id(transaction.getId())
                .cashierName(cashierName)
                .transactionType(type)
                .localizedTransactionType(type != null ? type.getLocalizedName(messageService) : null)
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency() != null ? transaction.getCurrency() : "RUB")
                .paymentMethod(transaction.getPaymentMethod())
                .localizedPaymentMethod(transaction.getPaymentMethod() != null ?
                        transaction.getPaymentMethod().getLocalizedName(messageService) : null)
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .sign(type != null ? type.getSign() : null)
                .build();
    }

    /**
     * Конвертирует сущность в DTO с дополнительной информацией
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
                BigDecimal balanceAfter = balanceBefore.add(
                        transaction.getAmount().multiply(
                                BigDecimal.valueOf(transaction.getTransactionType().getMultiplier())
                        )
                );
                dto.setBalanceAfter(balanceAfter);
            }
        }

        return dto;
    }
}