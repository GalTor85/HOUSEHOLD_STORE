package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.UserTypeAssignmentNotFoundException;
import ru.galtor85.household_store.dto.UserTypeAssignmentDto;
import ru.galtor85.household_store.entity.UserType;
import ru.galtor85.household_store.entity.UserTypeAssignment;
import ru.galtor85.household_store.mapper.UserTypeAssignmentMapper;
import ru.galtor85.household_store.repository.UserTypeAssignmentRepository;
import ru.galtor85.household_store.service.MessageService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssignmentQueryProcessor {

    private final UserTypeAssignmentRepository assignmentRepository;
    private final UserTypeAssignmentMapper mapper;
    private final MessageService messageService;

    @Transactional(readOnly = true)
    public UserTypeAssignmentDto getCurrentUserType(Long userId) {
        return assignmentRepository.findActiveByUserId(userId)
                .map(mapper::toDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public UserTypeAssignment getCurrentUserTypeEntity(Long userId) {
        return assignmentRepository.findActiveByUserId(userId).orElse(null);
    }

    @Transactional(readOnly = true)
    public UserTypeAssignmentDto getCurrentUserTypeOrThrow(Long userId) {
        return assignmentRepository.findActiveByUserId(userId)
                .map(mapper::toDto)
                .orElseThrow(() -> {
                    log.warn(messageService.get("user-type.log.not.found", userId));
                    return new UserTypeAssignmentNotFoundException(userId);
                });
    }

    @Transactional(readOnly = true)
    public UserTypeAssignment getCurrentUserTypeEntityOrThrow(Long userId) {
        return assignmentRepository.findActiveByUserId(userId)
                .orElseThrow(() -> {
                    log.warn(messageService.get("user-type.log.not.found", userId));
                    return new UserTypeAssignmentNotFoundException(userId);
                });
    }

    @Transactional(readOnly = true)
    public List<UserTypeAssignmentDto> getUserTypeHistory(Long userId) {
        List<UserTypeAssignment> history = assignmentRepository.findByUserId(userId);
        log.debug(messageService.get("user-type.log.history.size", userId, history.size()));
        return mapper.toDtoList(history);
    }

    @Transactional(readOnly = true)
    public List<UserTypeAssignment> getUserTypeHistoryEntity(Long userId) {
        List<UserTypeAssignment> history = assignmentRepository.findByUserId(userId);
        log.debug(messageService.get("user-type.log.history.size", userId, history.size()));
        return history;
    }

    @Transactional(readOnly = true)
    public boolean hasActiveUserType(Long userId, UserType userType) {
        return assignmentRepository.findActiveByUserId(userId)
                .map(assignment -> assignment.getUserType() == userType)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<Long> getUserIdsByUserType(UserType userType) {
        return assignmentRepository.findActiveByUserType(userType)
                .stream()
                .map(UserTypeAssignment::getUserId)
                .collect(Collectors.toList());
    }
}