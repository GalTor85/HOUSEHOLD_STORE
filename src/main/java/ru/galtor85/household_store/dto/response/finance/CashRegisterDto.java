package ru.galtor85.household_store.dto.response.finance;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Cash register DTO")
public class CashRegisterDto {

    // =========================================================================
    // ИДЕНТИФИКАТОРЫ
    // =========================================================================

    @Schema(description = "Cash register ID", example = "1")
    private Long id;

    @Schema(description = "Cash register number", example = "REG-001")
    private String registerNumber;

    @Schema(description = "Cash register name", example = "Основная касса")
    private String name;

    @Schema(description = "Location", example = "Главный зал")
    private String location;

    // =========================================================================
    // СТАТУС
    // =========================================================================

    @Schema(description = "Is cash register active", example = "true")
    private Boolean isActive;

    @Schema(description = "Localized status", example = "Активна")
    private String localizedStatus;

    // =========================================================================
    // БАЛАНСЫ
    // =========================================================================

    @Schema(description = "Opening balance", example = "10000.00")
    private BigDecimal openingBalance;

    @Schema(description = "Closing balance", example = "15000.00")
    private BigDecimal closingBalance;

    @Schema(description = "Current balance (for active cash registers)", example = "12500.00")
    private BigDecimal currentBalance;

    @Schema(description = "Discrepancy", example = "100.00")
    private BigDecimal discrepancy;

    // =========================================================================
    // КАССИР
    // =========================================================================

    @Schema(description = "Cashier ID", example = "1")
    private Long cashierId;

    @Schema(description = "Cashier name", example = "Иванов Иван")
    private String cashierName;

    @Schema(description = "Cashier email", example = "ivan@example.com")
    private String cashierEmail;

    // =========================================================================
    // ДАТЫ
    // =========================================================================

    @Schema(description = "Opened at timestamp")
    private LocalDateTime openedAt;

    @Schema(description = "Closed at timestamp")
    private LocalDateTime closedAt;

    @Schema(description = "Created at timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Updated at timestamp")
    private LocalDateTime updatedAt;

    // =========================================================================
    // СТАТИСТИКА (опциональные поля)
    // =========================================================================

    @Schema(description = "Total income for today", example = "5000.00")
    private BigDecimal todayIncome;

    @Schema(description = "Total expense for today", example = "2000.00")
    private BigDecimal todayExpense;

    @Schema(description = "Number of transactions today", example = "15")
    private Integer todayTransactionCount;

    // =========================================================================
    // ЛОКАЛИЗОВАННЫЕ ПОЛЯ ДЛЯ UI
    // =========================================================================

    @Schema(description = "Localized opening balance", example = "Начальный остаток: 10 000.00 ₽")
    private String localizedOpeningBalance;

    @Schema(description = "Localized closing balance", example = "Конечный остаток: 15 000.00 ₽")
    private String localizedClosingBalance;

    @Schema(description = "Localized current balance", example = "Текущий остаток: 12 500.00 ₽")
    private String localizedCurrentBalance;

    @Schema(description = "Localized today income", example = "Приход за сегодня: 5 000.00 ₽")
    private String localizedTodayIncome;

    @Schema(description = "Localized today expense", example = "Расход за сегодня: 2 000.00 ₽")
    private String localizedTodayExpense;

    @Schema(description = "Localized opened at", example = "Открыта: 25.03.2024 09:00")
    private String localizedOpenedAt;

    @Schema(description = "Localized closed at", example = "Закрыта: 25.03.2024 18:00")
    private String localizedClosedAt;

    @Schema(description = "Localized created at", example = "Создана: 25.03.2024 08:00")
    private String localizedCreatedAt;

    @Schema(description = "Localized discrepancy", example = "Излишек: 100.00 ₽")
    private String localizedDiscrepancy;

    @Schema(description = "Localized discrepancy reason", example = "Ошибка кассира при подсчёте")
    private String localizedDiscrepancyReason;

    // =========================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =========================================================================

    /**
     * Проверяет, активна ли касса
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    /**
     * Получает форматированное название для отображения
     */
    public String getDisplayName() {
        return name != null ? name : registerNumber;
    }

    /**
     * Получает полную информацию о кассе для отображения
     */
    public String getFullInfo() {
        return String.format("%s (%s) - %s",
                getDisplayName(),
                registerNumber,
                isActive() ? "Активна" : "Закрыта");
    }

    /**
     * Инициализирует локализованные поля
     */
    public void initLocalizedFields(MessageService messageService, String currency) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        String currencySymbol = getCurrencySymbol(currency);

        if (openingBalance != null) {
            localizedOpeningBalance = messageService.get("cash.register.balance.opening",
                    formatBalance(openingBalance), currencySymbol);
        }

        if (closingBalance != null) {
            localizedClosingBalance = messageService.get("cash.register.balance.closing",
                    formatBalance(closingBalance), currencySymbol);
        }

        if (currentBalance != null) {
            localizedCurrentBalance = messageService.get("cash.register.balance.current",
                    formatBalance(currentBalance), currencySymbol);
        }

        if (todayIncome != null) {
            localizedTodayIncome = messageService.get("cash.register.today.income",
                    formatBalance(todayIncome), currencySymbol);
        }

        if (todayExpense != null) {
            localizedTodayExpense = messageService.get("cash.register.today.expense",
                    formatBalance(todayExpense), currencySymbol);
        }

        if (openedAt != null) {
            localizedOpenedAt = messageService.get("cash.register.opened.at",
                    openedAt.format(dateFormatter));
        }

        if (closedAt != null) {
            localizedClosedAt = messageService.get("cash.register.closed.at",
                    closedAt.format(dateFormatter));
        }

        if (createdAt != null) {
            localizedCreatedAt = messageService.get("cash.register.created.at",
                    createdAt.format(dateFormatter));
        }
        if (discrepancy != null) {
            if (discrepancy.compareTo(BigDecimal.ZERO) == 0) {
                localizedDiscrepancy = messageService.get("cash.register.discrepancy.none");
            } else if (discrepancy.compareTo(BigDecimal.ZERO) > 0) {
                localizedDiscrepancy = messageService.get("cash.register.discrepancy.excess",
                        formatBalance(discrepancy), currencySymbol);
            } else {
                localizedDiscrepancy = messageService.get("cash.register.discrepancy.shortage",
                        formatBalance(discrepancy.abs()), currencySymbol);
            }
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
     * Получает символ валюты
     */
    private String getCurrencySymbol(String currency) {
        return switch (currency.toUpperCase()) {
            case "RUB" -> "₽";
            case "USD" -> "$";
            case "EUR" -> "€";
            default -> currency;
        };
    }
}