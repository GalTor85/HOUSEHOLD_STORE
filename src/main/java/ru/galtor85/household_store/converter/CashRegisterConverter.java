package ru.galtor85.household_store.converter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.finance.CashRegisterDto;
import ru.galtor85.household_store.entity.finance.CashRegister;
import ru.galtor85.household_store.repository.cash.CashTransactionRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CashRegisterConverter {

    private final MessageService messageService;
    private final CashTransactionRepository cashTransactionRepository;

    /**
     * Конвертирует сущность кассы в DTO
     */
    public CashRegisterDto toDto(CashRegister cashRegister) {
        if (cashRegister == null) {
            return null;
        }

        BigDecimal currentBalance = null;
        if (cashRegister.getIsActive()) {
            currentBalance = calculateCurrentBalance(cashRegister);
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
                .currentBalance(currentBalance)
                .cashierId(cashRegister.getCashierId())
                .openedAt(cashRegister.getOpenedAt())
                .closedAt(cashRegister.getClosedAt())
                .createdAt(cashRegister.getCreatedAt())
                .updatedAt(cashRegister.getUpdatedAt())
                .build();
    }

    /**
     * Конвертирует список касс в список DTO
     */
    public List<CashRegisterDto> toDtoList(List<CashRegister> cashRegisters) {
        if (cashRegisters == null) {
            return null;
        }
        return cashRegisters.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Рассчитывает текущий баланс кассы
     */
    private BigDecimal calculateCurrentBalance(CashRegister cashRegister) {
        BigDecimal totalIncome = cashTransactionRepository.getTotalAmountByCashRegister(cashRegister.getId());
        if (totalIncome == null) {
            totalIncome = BigDecimal.ZERO;
        }

        BigDecimal totalExpense = cashTransactionRepository.getTotalExpenseByCashRegisterAndDateRange(
                cashRegister.getId(), cashRegister.getOpenedAt(), java.time.LocalDateTime.now());
        if (totalExpense == null) {
            totalExpense = BigDecimal.ZERO;
        }

        return cashRegister.getOpeningBalance()
                .add(totalIncome)
                .subtract(totalExpense);
    }
}