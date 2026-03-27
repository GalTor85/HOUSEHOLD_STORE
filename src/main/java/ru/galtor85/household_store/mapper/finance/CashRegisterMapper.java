package ru.galtor85.household_store.mapper.finance;

import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.request.finance.CashRegisterCreateRequest;
import ru.galtor85.household_store.dto.request.finance.CashRegisterUpdateRequest;
import ru.galtor85.household_store.entity.finance.CashRegister;

import java.math.BigDecimal;

@Component
public class CashRegisterMapper {

    /**
     * Преобразует запрос на создание в сущность
     */
    public CashRegister toEntity(CashRegisterCreateRequest request, Long createdBy) {
        if (request == null) {
            return null;
        }

        return CashRegister.builder()
                .registerNumber(request.getRegisterNumber())
                .name(request.getName())
                .location(request.getLocation())
                .openingBalance(request.getOpeningBalance() != null ? request.getOpeningBalance() : BigDecimal.ZERO)
                .isActive(false)
                .createdBy(createdBy)
                .build();
    }

    /**
     * Обновляет сущность из запроса на обновление
     */
    public void updateEntity(CashRegister cashRegister, CashRegisterUpdateRequest request) {
        if (cashRegister == null || request == null) {
            return;
        }

        if (request.getName() != null) {
            cashRegister.setName(request.getName());
        }
        if (request.getLocation() != null) {
            cashRegister.setLocation(request.getLocation());
        }
        // Примечание: openingBalance не обновляется через этот метод,
        // так как начальный баланс устанавливается только при открытии кассы
    }

    /**
     * Обновляет начальный баланс кассы (при открытии)
     */
    public void updateOpeningBalance(CashRegister cashRegister, BigDecimal openingBalance) {
        if (cashRegister != null && openingBalance != null) {
            cashRegister.setOpeningBalance(openingBalance);
        }
    }

    /**
     * Создает копию сущности
     */
    public CashRegister copy(CashRegister source) {
        if (source == null) {
            return null;
        }

        return CashRegister.builder()
                .registerNumber(source.getRegisterNumber())
                .name(source.getName())
                .location(source.getLocation())
                .openingBalance(source.getOpeningBalance())
                .isActive(source.getIsActive())
                .cashierId(source.getCashierId())
                .openedAt(source.getOpenedAt())
                .closedAt(source.getClosedAt())
                .createdBy(source.getCreatedBy())
                .build();
    }
}