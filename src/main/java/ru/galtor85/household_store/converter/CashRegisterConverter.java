package ru.galtor85.household_store.converter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.finance.CashRegisterDto;
import ru.galtor85.household_store.entity.finance.CashRegister;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;

/**
 * Converter for transforming CashRegister entities to DTOs
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CashRegisterConverter {

    private final MessageService messageService;

    // =========================================================================
    // SINGLE ENTITY CONVERSION
    // =========================================================================

    /**
     * Converts CashRegister entity to DTO with current balance
     *
     * @param cashRegister the cash register entity
     * @param currentBalance the calculated current balance (provided by service)
     * @return CashRegisterDto with all fields including current balance
     */
    public CashRegisterDto toDto(CashRegister cashRegister, BigDecimal currentBalance) {
        if (cashRegister == null) {
            return null;
        }

        BigDecimal discrepancy = null;
        if (cashRegister.getClosingBalance() != null && currentBalance != null) {
            discrepancy = cashRegister.getClosingBalance().subtract(currentBalance);
        }

        return CashRegisterDto.builder()
                .id(cashRegister.getId())
                .registerNumber(cashRegister.getRegisterNumber())
                .name(cashRegister.getName())
                .location(cashRegister.getLocation())
                .isActive(cashRegister.getIsActive())
                .localizedStatus(cashRegister.getIsActive() ?
                        messageService.get("cash.register.status.active") :
                        messageService.get("cash.register.status.inactive"))
                .openingBalance(cashRegister.getOpeningBalance())
                .closingBalance(cashRegister.getClosingBalance())
                .discrepancy(discrepancy)
                .currentBalance(currentBalance)
                .cashierId(cashRegister.getCashierId())
                .openedAt(cashRegister.getOpenedAt())
                .closedAt(cashRegister.getClosedAt())
                .createdAt(cashRegister.getCreatedAt())
                .updatedAt(cashRegister.getUpdatedAt())
                .build();
    }

    /**
     * Converts CashRegister entity to simplified DTO without current balance
     *
     * @param cashRegister the cash register entity
     * @return simplified CashRegisterDto
     */
    public CashRegisterDto toSimpleDto(CashRegister cashRegister) {
        if (cashRegister == null) {
            return null;
        }

        return CashRegisterDto.builder()
                .id(cashRegister.getId())
                .registerNumber(cashRegister.getRegisterNumber())
                .name(cashRegister.getName())
                .location(cashRegister.getLocation())
                .isActive(cashRegister.getIsActive())
                .localizedStatus(cashRegister.getIsActive() ?
                        messageService.get("cash.register.status.active") :
                        messageService.get("cash.register.status.inactive"))
                .openingBalance(cashRegister.getOpeningBalance())
                .closingBalance(cashRegister.getClosingBalance())
                .cashierId(cashRegister.getCashierId())
                .openedAt(cashRegister.getOpenedAt())
                .closedAt(cashRegister.getClosedAt())
                .createdAt(cashRegister.getCreatedAt())
                .updatedAt(cashRegister.getUpdatedAt())
                .build();
    }
}