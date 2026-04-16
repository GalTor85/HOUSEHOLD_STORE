package ru.galtor85.household_store.mapper.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.user.UserTypeAssignmentDto;
import ru.galtor85.household_store.entity.user.UserType;
import ru.galtor85.household_store.entity.user.UserTypeAssignment;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.time.LocalDateTime;

/**
 * Mapper for user type assignment entity to/from DTO.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserTypeAssignmentMapper {

    private final MessageService messageService;

    /**
     * Converts entity to DTO with localization.
     *
     * @param assignment user type assignment entity
     * @return user type assignment DTO
     */
    public UserTypeAssignmentDto toDto(UserTypeAssignment assignment) {
        if (assignment == null) {
            return null;
        }
        String localizedName = messageService.get("usertype." + assignment.getUserType().name().toLowerCase());

        return UserTypeAssignmentDto.builder()
                .id(assignment.getId())
                .userId(assignment.getUserId())
                .userType(assignment.getUserType())
                .userTypeName(localizedName)
                .assignedAt(assignment.getAssignedAt())
                .updatedAt(assignment.getUpdatedAt())
                .assignedBy(assignment.getAssignedBy())
                .active(assignment.isActive())
                .validFrom(assignment.getValidFrom())
                .validTo(assignment.getValidTo())
                .reason(assignment.getReason())
                .build();
    }

    /**
     * Creates entity from parameters.
     *
     * @param userId user ID
     * @param userType user type
     * @param assignedBy who assigned
     * @param reason assignment reason
     * @param validFrom validity start
     * @param validTo validity end
     * @return user type assignment entity
     */
    public UserTypeAssignment createEntity(Long userId, UserType userType, String assignedBy,
                                           String reason, LocalDateTime validFrom, LocalDateTime validTo) {
        return UserTypeAssignment.builder()
                .userId(userId)
                .userType(userType)
                .assignedBy(assignedBy)
                .reason(reason)
                .validFrom(validFrom != null ? validFrom : LocalDateTime.now())
                .validTo(validTo)
                .active(true)
                .build();
    }

    /**
     * Deactivates assignment.
     *
     * @param assignment assignment to deactivate
     * @return deactivated assignment
     */
    public UserTypeAssignment deactivate(UserTypeAssignment assignment) {
        if (assignment == null) {
            return null;
        }
        assignment.setActive(false);
        assignment.setUpdatedAt(LocalDateTime.now());
        return assignment;
    }
}