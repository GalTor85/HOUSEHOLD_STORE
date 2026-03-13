package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.UserType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User type assignment DTO", title = "User Type Assignment")
public class UserTypeAssignmentDto {

    @Schema(description = "Assignment ID", example = "1")
    private Long id;

    @Schema(description = "User ID", example = "1")
    private Long userId;

    @Schema(description = "User type", example = "RETAIL")
    private UserType userType;

    @Schema(description = "Localized user type name", example = "Розничный покупатель")
    private String userTypeName;

    @Schema(description = "Assignment timestamp")
    private LocalDateTime assignedAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    @Schema(description = "Assigned by", example = "admin@example.com")
    private String assignedBy;

    @Schema(description = "Is active", example = "true")
    private boolean active;

    @Schema(description = "Valid from")
    private LocalDateTime validFrom;

    @Schema(description = "Valid to")
    private LocalDateTime validTo;

    @Schema(description = "Reason", example = "Initial assignment")
    private String reason;
}