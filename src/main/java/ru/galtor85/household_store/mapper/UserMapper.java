package ru.galtor85.household_store.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.UserResponse;
import ru.galtor85.household_store.dto.UserTypeAssignmentDto;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.entity.UserTypeAssignment;
import ru.galtor85.household_store.repository.SecurityUserRepository;
import ru.galtor85.household_store.repository.UserTypeAssignmentRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.MessageService;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final SecurityUserRepository securityUserRepository;
    private final UserTypeAssignmentRepository userTypeAssignmentRepository;
    private final MessageService messageService;

    public UserResponse build(User user) {
        if (user == null) {
            return null;
        }

        SecurityUser securityUser = securityUserRepository.findById(user.getId())
                .orElse(null);

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

        // ДОБАВЛЕНО: получаем текущий тип пользователя и конвертируем в DTO
        userTypeAssignmentRepository.findActiveByUserId(user.getId())
                .ifPresent(assignment -> {
                    UserTypeAssignmentDto assignmentDto = convertToDto(assignment);
                    response.setCurrentUserType(assignmentDto);
                });

        if (securityUser != null) {
            response.setRole(securityUser.getRole());
            response.setActive(securityUser.isEnabled());
        }

        response.setCreator(user.getCreator());

        if (user.getCreatedAt() != null) {
            response.setCreatedAt(user.getCreatedAt().toString());
        }

        if (user.getUpdatedAt() != null) {
            response.setUpdatedAt(user.getUpdatedAt().toString());
        }

        return response;
    }

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

        //Получаем текущий тип пользователя и конвертируем в DTO
        userTypeAssignmentRepository.findActiveByUserId(user.getId())
                .ifPresent(assignment -> {
                    UserTypeAssignmentDto assignmentDto = convertToDto(assignment);
                    response.setCurrentUserType(assignmentDto);
                });

        if (securityUser != null) {
            response.setRole(securityUser.getRole());
            response.setActive(securityUser.isEnabled());
        }

        response.setCreator(user.getCreator());

        if (user.getCreatedAt() != null) {
            response.setCreatedAt(user.getCreatedAt().toString());
        }

        if (user.getUpdatedAt() != null) {
            response.setUpdatedAt(user.getUpdatedAt().toString());
        }

        return response;
    }

    private UserTypeAssignmentDto convertToDto(UserTypeAssignment assignment) {
        String localizedName = messageService.get(
                "usertype." + assignment.getUserType().name()
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
}