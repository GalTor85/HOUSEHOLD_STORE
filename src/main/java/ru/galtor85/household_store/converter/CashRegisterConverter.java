package ru.galtor85.household_store.converter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.finance.CashRegisterDto;
import ru.galtor85.household_store.entity.finance.CashRegister;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    // =========================================================================
    // LIST CONVERSION
    // =========================================================================

    /**
     * Converts a list of cash registers to simplified DTOs (without current balance)
     */
    public List<CashRegisterDto> toDtoList(List<CashRegister> cashRegisters) {
        if (cashRegisters == null) {
            return null;
        }
        return cashRegisters.stream()
                .map(this::toSimpleDto)
                .collect(Collectors.toList());
    }



    /**
     * Converts a list of cash registers to DTOs with current balances
     *
     * @param cashRegisters list of cash register entities
     * @param currentBalances map of register ID to current balance (provided by service)
     * @return list of CashRegisterDto with current balances
     */
    public List<CashRegisterDto> toDtoListWithBalance(List<CashRegister> cashRegisters,
                                           Map<Long, BigDecimal> currentBalances) {
        if (cashRegisters == null) {
            return null;
        }
        return cashRegisters.stream()
                .map(register -> {
                    BigDecimal currentBalance = currentBalances.get(register.getId());
                    return toDto(register, currentBalance);
                })
                .collect(Collectors.toList());
    }
}