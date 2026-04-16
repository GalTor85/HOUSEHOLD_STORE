package ru.galtor85.household_store.mapper.finance;

import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.request.finance.CashRegisterCreateRequest;
import ru.galtor85.household_store.dto.request.finance.CashRegisterUpdateRequest;
import ru.galtor85.household_store.entity.finance.CashRegister;

import java.math.BigDecimal;

/**
 * Mapper for cash register entity to/from DTO.
 */
@Component
public class CashRegisterMapper {



    /**
     * Converts creation request to entity.
     *
     * @param request creation request
     * @param createdBy ID of user creating
     * @return cash register entity
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
     * Updates entity from update request.
     *
     * @param cashRegister existing entity
     * @param request update request
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
    }
}