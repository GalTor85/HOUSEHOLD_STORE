package ru.galtor85.household_store.validator.rollback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.rollback.RollbackAlreadyPendingException;
import ru.galtor85.household_store.advice.exception.rollback.RollbackAlreadyProcessedException;
import ru.galtor85.household_store.advice.exception.rollback.RollbackFinalStatusException;
import ru.galtor85.household_store.entity.rollback.ApprovalStatus;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.entity.rollback.RollbackApproval;
import ru.galtor85.household_store.repository.rollback.RollbackApprovalRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class RollbackValidator {

    private final RollbackApprovalRepository approvalRepository;
    private final MessageService messageService;

    public void validateNoPendingRollback(Long orderId) {
        if (approvalRepository.existsByOrderIdAndApprovalStatus(orderId, ApprovalStatus.PENDING)) {
            log.warn(messageService.get("rollback.log.already.pending", orderId));
            throw new RollbackAlreadyPendingException(orderId);
        }
    }

    public void validateRollbackPossibility(SalesOrder salesOrder) {
        OrderStatus status = salesOrder.getStatus();

        if (status == OrderStatus.COMPLETED ||
                status == OrderStatus.CANCELLED ||
                status == OrderStatus.REFUNDED ||
                status == OrderStatus.RETURNED) {

            log.warn(messageService.get("rollback.error.final.status", status));
            throw new RollbackFinalStatusException(status);
        }
    }

    public void validateApprovalPending(RollbackApproval approval) {
        if (approval.getApprovalStatus() != ApprovalStatus.PENDING) {
            log.warn(messageService.get("rollback.error.already.processed", approval.getId(), approval.getApprovalStatus()));
            throw new RollbackAlreadyProcessedException();
        }
    }
}