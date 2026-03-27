package ru.galtor85.household_store.processor.rollback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.rollback.ApprovalStatus;
import ru.galtor85.household_store.entity.rollback.RollbackApproval;
import ru.galtor85.household_store.repository.rollback.RollbackApprovalRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class RollbackDecisionProcessor {

    private final RollbackApprovalRepository approvalRepository;
    private final RollbackExecutionProcessor executionProcessor;
    private final MessageService messageService;

    @Transactional
    public RollbackApproval approve(RollbackApproval approval, String adminComments,
                                    Long adminId) {

        log.info(messageService.get("rollback.decision.approve.start",
                approval.getId(), adminId));

        approval.setApprovalStatus(ApprovalStatus.APPROVED);
        approval.setReviewedById(adminId);
        approval.setAdminComments(adminComments);
        approval.setReviewedAt(LocalDateTime.now());

        RollbackApproval updated = approvalRepository.save(approval);

        log.info(messageService.get("rollback.decision.approve.success",
                approval.getId(), adminId));

        return updated;
    }

    @Transactional
    public RollbackApproval reject(RollbackApproval approval, String adminComments,
                                   Long adminId) {

        log.info(messageService.get("rollback.decision.reject.start",
                approval.getId(), adminId));

        approval.setApprovalStatus(ApprovalStatus.REJECTED);
        approval.setReviewedById(adminId);
        approval.setAdminComments(adminComments);
        approval.setReviewedAt(LocalDateTime.now());

        RollbackApproval updated = approvalRepository.save(approval);

        log.info(messageService.get("rollback.decision.reject.success",
                approval.getId(), adminId));

        return updated;
    }
}