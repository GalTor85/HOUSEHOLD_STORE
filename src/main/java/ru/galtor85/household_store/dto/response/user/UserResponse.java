package ru.galtor85.household_store.dto.response.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.user.Role;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User response DTO", title = "User Response")
public class UserResponse {

    @Schema(description = "User ID", example = "1")
    private Long id;

    @Schema(description = "User email", example = "user@example.com")
    private String email;

    @Schema(description = "User first name", example = "John")
    private String firstName;

    @Schema(description = "User last name", example = "Doe")
    private String lastName;

    @Schema(description = "User surname (patronymic)", example = "Smith")
    private String surname;

    @Schema(description = "User address", example = "123 Main St, City")
    private String address;

    @Schema(description = "User mobile number", example = "+1234567890")
    private String mobileNumber;

    @Schema(description = "Birth date", example = "1990-01-01")
    private LocalDate birthDate;

    @Schema(description = "Age (calculated from birth date)", example = "34")
    private Integer age;

    @Schema(description = "User role", example = "USER")
    private Role role;

    @Schema(description = "Creator of the user", example = "admin@example.com")
    private String creator;

    @Schema(description = "User active status", example = "true")
    private boolean active;

    @Schema(description = "Creation timestamp", example = "2024-01-01T10:30:00")
    private String createdAt;

    @Schema(description = "Last update timestamp", example = "2024-01-02T15:45:00")
    private String updatedAt;

    @Schema(description = "User type assignments")
    private List<UserTypeAssignmentDto> userTypeAssignments;

    @Schema(description = "Current active user type")
    private UserTypeAssignmentDto currentUserType;
}
