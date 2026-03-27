package ru.galtor85.household_store.mapper.rollback;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.order.RollbackApprovalDto;
import ru.galtor85.household_store.entity.rollback.RollbackApproval;
import ru.galtor85.household_store.repository.user.UserRepository;

@Component
@RequiredArgsConstructor
public class RollbackApprovalMapper {

    private final UserRepository userRepository;

    public RollbackApprovalDto toDto(RollbackApproval approval) {
        if (approval == null) return null;

        String requestedByEmail = userRepository.findById(approval.getRequestedById())
                .map(user -> user.getEmail())
                .orElse(null);

        String reviewedByEmail = approval.getReviewedById() != null ?
                userRepository.findById(approval.getReviewedById())
                        .map(user -> user.getEmail())
                        .orElse(null) : null;

        return RollbackApprovalDto.builder()
                .id(approval.getId())
                .orderId(approval.getOrderId())
                .currentStatus(approval.getCurrentStatus())
                .targetStatus(approval.getTargetStatus())
                .requestedBy(requestedByEmail)
                .requestedById(approval.getRequestedById())
                .reason(approval.getReason())
                .comments(approval.getComments())
                .requestedAt(approval.getRequestedAt())
                .approvalStatus(approval.getApprovalStatus().name())
                .reviewedBy(reviewedByEmail)
                .reviewedAt(approval.getReviewedAt())
                .adminComments(approval.getAdminComments())
                .build();
    }
}