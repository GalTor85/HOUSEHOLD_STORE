package ru.galtor85.household_store.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.SupplierCreateRequest;
import ru.galtor85.household_store.dto.SupplierDto;
import ru.galtor85.household_store.dto.SupplierUpdateRequest;
import ru.galtor85.household_store.entity.Supplier;
import ru.galtor85.household_store.entity.SupplierStatus;
import ru.galtor85.household_store.service.MessageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupplierMapper {

    private final MessageService messageService;

    /**
     * Преобразование запроса на создание в сущность Supplier
     */
    public Supplier toEntity(SupplierCreateRequest request, Long creatorId) {
        if (request == null) {
            return null;
        }

        return Supplier.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .website(request.getWebsite())
                .contactPerson(request.getContactPerson())
                .inn(request.getInn())
                .kpp(request.getKpp())
                .ogrn(request.getOgrn())
                .legalAddress(request.getLegalAddress())
                .actualAddress(request.getActualAddress())
                .bankName(request.getBankName())
                .bankBic(request.getBankBic())
                .bankAccount(request.getBankAccount())
                .correspondentAccount(request.getCorrespondentAccount())
                .status(SupplierStatus.PENDING)
                .deliveryTime(request.getDeliveryTime())
                .minOrderAmount(request.getMinOrderAmount())
                .paymentDelay(request.getPaymentDelay())
                .createdBy(creatorId)
                .build();
    }

    /**
     * Обновление сущности из запроса на обновление
     */
    public void updateEntity(Supplier supplier, SupplierUpdateRequest request) {
        if (supplier == null || request == null) {
            return;
        }

        if (request.getName() != null) {
            supplier.setName(request.getName());
        }
        if (request.getEmail() != null) {
            supplier.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            supplier.setPhone(request.getPhone());
        }
        if (request.getWebsite() != null) {
            supplier.setWebsite(request.getWebsite());
        }
        if (request.getContactPerson() != null) {
            supplier.setContactPerson(request.getContactPerson());
        }
        if (request.getInn() != null) {
            supplier.setInn(request.getInn());
        }
        if (request.getKpp() != null) {
            supplier.setKpp(request.getKpp());
        }
        if (request.getOgrn() != null) {
            supplier.setOgrn(request.getOgrn());
        }
        if (request.getLegalAddress() != null) {
            supplier.setLegalAddress(request.getLegalAddress());
        }
        if (request.getActualAddress() != null) {
            supplier.setActualAddress(request.getActualAddress());
        }
        if (request.getBankName() != null) {
            supplier.setBankName(request.getBankName());
        }
        if (request.getBankBic() != null) {
            supplier.setBankBic(request.getBankBic());
        }
        if (request.getBankAccount() != null) {
            supplier.setBankAccount(request.getBankAccount());
        }
        if (request.getCorrespondentAccount() != null) {
            supplier.setCorrespondentAccount(request.getCorrespondentAccount());
        }
        if (request.getStatus() != null) {
            try {
                supplier.setStatus(SupplierStatus.valueOf(request.getStatus()));
            } catch (IllegalArgumentException e) {
                log.warn(messageService.get("supplier.mapper.invalid.status", request.getStatus()));
            }
        }
        if (request.getDeliveryTime() != null) {
            supplier.setDeliveryTime(request.getDeliveryTime());
        }
        if (request.getMinOrderAmount() != null) {
            supplier.setMinOrderAmount(request.getMinOrderAmount());
        }
        if (request.getPaymentDelay() != null) {
            supplier.setPaymentDelay(request.getPaymentDelay());
        }
    }

    /**
     * Преобразование сущности в DTO
     */
    public SupplierDto toDto(Supplier supplier) {
        if (supplier == null) {
            return null;
        }

        String localizedStatus = messageService.get("supplier.status." + supplier.getStatus().name());

        return SupplierDto.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .email(supplier.getEmail())
                .phone(supplier.getPhone())
                .website(supplier.getWebsite())
                .contactPerson(supplier.getContactPerson())
                .inn(supplier.getInn())
                .kpp(supplier.getKpp())
                .ogrn(supplier.getOgrn())
                .legalAddress(supplier.getLegalAddress())
                .actualAddress(supplier.getActualAddress())
                .bankName(supplier.getBankName())
                .bankBic(supplier.getBankBic())
                .bankAccount(supplier.getBankAccount())
                .correspondentAccount(supplier.getCorrespondentAccount())
                .status(supplier.getStatus())
                .localizedStatus(localizedStatus)
                .rating(supplier.getRating())
                .ratingCount(supplier.getRatingCount())
                .deliveryTime(supplier.getDeliveryTime())
                .minOrderAmount(supplier.getMinOrderAmount())
                .paymentDelay(supplier.getPaymentDelay())
                .createdBy(supplier.getCreatedBy())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }
}