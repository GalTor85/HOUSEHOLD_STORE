package ru.galtor85.household_store.dto.request.warehouse;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Warehouse update request DTO")
public class WarehouseUpdateRequest {

    @Size(max = MAX_WAREHOUSE_CODE_LENGTH, message = "{warehouse.validation.code.max}")
    @Schema(description = "Warehouse code", example = "WH-001")
    private String code;

    @Size(max = MAX_WAREHOUSE_NAME_LENGTH, message = "{warehouse.validation.name.max}")
    @Schema(description = "Warehouse name", example = "Main Warehouse")
    private String name;

    @Size(max = MAX_DESCRIPTION_LENGTH, message = "{warehouse.validation.description.max}")
    @Schema(description = "Description", example = "Main warehouse for finished goods")
    private String description;

    @Size(max = MAX_ADDRESS_LENGTH, message = "{warehouse.validation.address.max}")
    @Schema(description = "Physical address", example = "123 Storage St, City")
    private String address;

    @Size(max = MAX_CONTACT_PERSON_LENGTH, message = "{warehouse.validation.contact.person.max}")
    @Schema(description = "Contact person", example = "John Doe")
    private String contactPerson;

    @Size(max = MAX_PHONE_LENGTH, message = "{warehouse.validation.contact.phone.max}")
    @Schema(description = "Contact phone", example = "+1234567890")
    private String contactPhone;

    @Email(message = "{warehouse.validation.contact.email.invalid}")
    @Size(max = MAX_EMAIL_LENGTH, message = "{warehouse.validation.contact.email.max}")
    @Schema(description = "Contact email", example = "warehouse@company.com")
    private String contactEmail;

    @Schema(description = "Is active", example = "true")
    private Boolean isActive;

    @Schema(description = "Is visible for sale", example = "true")
    private Boolean isVisibleForSale;

    @Positive(message = "{warehouse.validation.total.capacity.positive}")
    @Schema(description = "Total capacity", example = "1000")
    private Integer totalCapacity;
}