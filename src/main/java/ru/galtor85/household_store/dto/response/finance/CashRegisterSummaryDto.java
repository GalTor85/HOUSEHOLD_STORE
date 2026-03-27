package ru.galtor85.household_store.dto.response.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Cash register summary DTO")
public class CashRegisterSummaryDto {

    // =========================================================================
    // ИДЕНТИФИКАТОРЫ
    // =========================================================================

    @Schema(description = "Cash register ID", example = "1")
    private Long cashRegisterId;

    @Schema(description = "Cash register name", example = "Основная касса")
    private String cashRegisterName;

    @Schema(description = "Cash register number", example = "REG-001")
    private String cashRegisterNumber;

    // =========================================================================
    // ПЕРИОД
    // =========================================================================

    @Schema(description = "Period start")
    private LocalDateTime startDate;

    @Schema(description = "Period end")
    private LocalDateTime endDate;

    @Schema(description = "Localized period", example = "25.03.2024 - 25.03.2024")
    private String localizedPeriod;

    // =========================================================================
    // БАЛАНСЫ
    // =========================================================================

    @Schema(description = "Opening balance", example = "10000.00")
    private BigDecimal openingBalance;

    @Schema(description = "Localized opening balance", example = "Начальный остаток: 10 000.00 ₽")
    private String localizedOpeningBalance;

    @Schema(description = "Closing balance", example = "15000.00")
    private BigDecimal closingBalance;

    @Schema(description = "Localized closing balance", example = "Конечный остаток: 15 000.00 ₽")
    private String localizedClosingBalance;

    // =========================================================================
    // ОБОРОТЫ
    // =========================================================================

    @Schema(description = "Total income", example = "5000.00")
    private BigDecimal totalIncome;

    @Schema(description = "Localized total income", example = "Приход: 5 000.00 ₽")
    private String localizedTotalIncome;

    @Schema(description = "Total expense", example = "2000.00")
    private BigDecimal totalExpense;

    @Schema(description = "Localized total expense", example = "Расход: 2 000.00 ₽")
    private String localizedTotalExpense;

    @Schema(description = "Net turnover (income - expense)", example = "3000.00")
    private BigDecimal netTurnover;

    @Schema(description = "Localized net turnover", example = "Чистый оборот: 3 000.00 ₽")
    private String localizedNetTurnover;

    // =========================================================================
    // СТАТИСТИКА ОПЕРАЦИЙ
    // =========================================================================

    @Schema(description = "Number of transactions", example = "15")
    private Integer transactionCount;

    @Schema(description = "Localized transaction count", example = "Количество операций: 15")
    private String localizedTransactionCount;

    @Schema(description = "Number of income transactions", example = "10")
    private Integer incomeCount;

    @Schema(description = "Number of expense transactions", example = "5")
    private Integer expenseCount;

    // =========================================================================
    // ДОПОЛНИТЕЛЬНЫЕ ПОЛЯ
    // =========================================================================

    @Schema(description = "Average transaction amount", example = "200.00")
    private BigDecimal averageTransactionAmount;

    @Schema(description = "Localized average transaction amount", example = "Средний чек: 200.00 ₽")
    private String localizedAverageTransactionAmount;

    @Schema(description = "Currency code", example = "RUB")
    private String currency;

    @Schema(description = "Currency symbol", example = "₽")
    private String currencySymbol;

    // =========================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =========================================================================

    /**
     * Инициализирует локализованные поля
     */
    public void initLocalizedFields(MessageService messageService, String currencySymbol) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        this.currencySymbol = currencySymbol;

        if (startDate != null && endDate != null) {
            localizedPeriod = messageService.get("cash.register.summary.period",
                    startDate.format(dateFormatter), endDate.format(dateFormatter));
        }

        if (openingBalance != null) {
            localizedOpeningBalance = messageService.get("cash.register.summary.opening.balance",
                    formatBalance(openingBalance), currencySymbol);
        }

        if (closingBalance != null) {
            localizedClosingBalance = messageService.get("cash.register.summary.closing.balance",
                    formatBalance(closingBalance), currencySymbol);
        }

        if (totalIncome != null) {
            localizedTotalIncome = messageService.get("cash.register.summary.total.income",
                    formatBalance(totalIncome), currencySymbol);
        }

        if (totalExpense != null) {
            localizedTotalExpense = messageService.get("cash.register.summary.total.expense",
                    formatBalance(totalExpense), currencySymbol);
        }

        if (netTurnover != null) {
            localizedNetTurnover = messageService.get("cash.register.summary.net.turnover",
                    formatBalance(netTurnover), currencySymbol);
        }

        if (transactionCount != null) {
            localizedTransactionCount = messageService.get("cash.register.summary.transaction.count",
                    transactionCount);
        }

        if (averageTransactionAmount != null) {
            localizedAverageTransactionAmount = messageService.get("cash.register.summary.average.amount",
                    formatBalance(averageTransactionAmount), currencySymbol);
        }
    }

    /**
     * Форматирует баланс
     */
    private String formatBalance(BigDecimal balance) {
        if (balance == null) {
            return "0.00";
        }
        return String.format("%,.2f", balance);
    }

    /**
     * Получает изменение баланса (closing - opening)
     */
    public BigDecimal getBalanceChange() {
        if (openingBalance == null || closingBalance == null) {
            return null;
        }
        return closingBalance.subtract(openingBalance);
    }

    /**
     * Получает локализованное изменение баланса
     */
    public String getLocalizedBalanceChange(MessageService messageService, String currencySymbol) {
        BigDecimal change = getBalanceChange();
        if (change == null) {
            return null;
        }
        String formatted = formatBalance(change.abs());
        if (change.compareTo(BigDecimal.ZERO) >= 0) {
            return messageService.get("cash.register.summary.balance.increase", formatted, currencySymbol);
        } else {
            return messageService.get("cash.register.summary.balance.decrease", formatted, currencySymbol);
        }
    }

    /**
     * Проверяет, есть ли операции за период
     */
    public boolean hasTransactions() {
        return transactionCount != null && transactionCount > 0;
    }

    /**
     * Получает процент доходов от общего оборота
     */
    public Double getIncomePercentage() {
        if (totalIncome == null || netTurnover == null || netTurnover.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return totalIncome.doubleValue() / netTurnover.doubleValue() * 100;
    }

    /**
     * Получает процент расходов от общего оборота
     */
    public Double getExpensePercentage() {
        if (totalExpense == null || netTurnover == null || netTurnover.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return totalExpense.doubleValue() / netTurnover.doubleValue() * 100;
    }
}