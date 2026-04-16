package ru.galtor85.household_store.mapper.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.user.UserResponse;
import ru.galtor85.household_store.dto.response.user.UserTypeAssignmentDto;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.entity.user.UserTypeAssignment;
import ru.galtor85.household_store.repository.auth.SecurityUserRepository;
import ru.galtor85.household_store.repository.user.UserTypeAssignmentRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.time.format.DateTimeFormatter;

/**
 * Mapper for user entity to response DTO.
 */
@Component
@RequiredArgsConstructor
public class UserMapper {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final SecurityUserRepository securityUserRepository;
    private final UserTypeAssignmentRepository userTypeAssignmentRepository;
    private final MessageService messageService;

    /**
     * Builds user response from user entity.
     *
     * @param user user entity
     * @return user response DTO
     */
    public UserResponse build(User user) {
        if (user == null) {
            return null;
        }
        SecurityUser securityUser = securityUserRepository.findById(user.getId()).orElse(null);
        return build(user, securityUser);
    }

    /**
     * Builds user response from user and security user entities.
     *
     * @param user user entity
     * @param securityUser security user entity
     * @return user response DTO
     */
    public UserResponse build(User user, SecurityUser securityUser) {
        if (user == null) {
            return null;
        }

        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setSurname(user.getSurname());
        response.setMobileNumber(user.getMobileNumber());
        response.setAddress(user.getAddress());
        response.setBirthDate(user.getBirthDate());
        response.setAge(user.getAge());
        response.setCreator(user.getCreator());

        userTypeAssignmentRepository.findActiveByUserId(user.getId())
                .ifPresent(assignment -> response.setCurrentUserType(convertToDto(assignment)));

        if (securityUser != null) {
            response.setRole(securityUser.getRole());
            response.setActive(securityUser.isEnabled());
        }

        if (user.getCreatedAt() != null) {
            response.setCreatedAt(user.getCreatedAt().format(ISO_FORMATTER));
        }
        if (user.getUpdatedAt() != null) {
            response.setUpdatedAt(user.getUpdatedAt().format(ISO_FORMATTER));
        }

        return response;
    }

    private UserTypeAssignmentDto convertToDto(UserTypeAssignment assignment) {
        String localizedName = messageService.get("usertype." + assignment.getUserType().name());

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
}