package ru.galtor85.household_store.mapper.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.user.UserTypeAssignmentDto;
import ru.galtor85.household_store.entity.user.UserType;
import ru.galtor85.household_store.entity.user.UserTypeAssignment;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserTypeAssignmentMapper {

    private final MessageService messageService;

    /**
     * Преобразование сущности в DTO с локализацией
     */
    public UserTypeAssignmentDto toDto(UserTypeAssignment assignment) {
        if (assignment == null) {
            return null;
        }

        String localizedName = messageService.get(
                "usertype." + assignment.getUserType().name().toLowerCase()
        );

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
     * Преобразование списка сущностей в список DTO
     */
    public List<UserTypeAssignmentDto> toDtoList(List<UserTypeAssignment> assignments) {
        if (assignments == null) {
            return null;
        }

        return assignments.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Преобразование DTO в сущность (для создания)
     */
    public UserTypeAssignment toEntity(UserTypeAssignmentDto dto) {
        if (dto == null) {
            return null;
        }

        return UserTypeAssignment.builder()
                .id(dto.getId())
                .userId(dto.getUserId())
                .userType(dto.getUserType())
                .assignedBy(dto.getAssignedBy())
                .reason(dto.getReason())
                .validFrom(dto.getValidFrom())
                .validTo(dto.getValidTo())
                .active(dto.isActive())
                .build();
    }

    /**
     * Создание сущности из параметров
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
     * Обновление существующей сущности
     */
    public void updateEntity(UserTypeAssignment assignment, UserTypeAssignmentDto dto) {
        if (assignment == null || dto == null) {
            return;
        }

        if (dto.getUserType() != null) {
            assignment.setUserType(dto.getUserType());
        }
        if (dto.getReason() != null) {
            assignment.setReason(dto.getReason());
        }
        if (dto.getValidFrom() != null) {
            assignment.setValidFrom(dto.getValidFrom());
        }
        if (dto.getValidTo() != null) {
            assignment.setValidTo(dto.getValidTo());
        }
    }

    /**
     * Деактивация назначения
     */
    public UserTypeAssignment deactivate(UserTypeAssignment assignment) {
        if (assignment == null) {
            return null;
        }
        assignment.setActive(false);
        assignment.setUpdatedAt(LocalDateTime.now());
        return assignment;
    }

    /**
     * Реактивация назначения
     */
    public UserTypeAssignment reactivate(UserTypeAssignment assignment) {
        if (assignment == null) {
            return null;
        }
        assignment.setActive(true);
        assignment.setUpdatedAt(LocalDateTime.now());
        return assignment;
    }
}