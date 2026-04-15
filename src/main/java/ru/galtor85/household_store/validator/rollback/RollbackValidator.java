package ru.galtor85.household_store.validator.rollback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.rollback.RollbackAlreadyPendingException;
import ru.galtor85.household_store.advice.exception.rollback.RollbackAlreadyProcessedException;
import ru.galtor85.household_store.advice.exception.rollback.RollbackFinalStatusException;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.entity.rollback.ApprovalStatus;
import ru.galtor85.household_store.entity.rollback.RollbackApproval;
import ru.galtor85.household_store.repository.rollback.RollbackApprovalRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;

/**
 * Validator for rollback operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RollbackValidator {

    private final RollbackApprovalRepository approvalRepository;
    private final LogMessageService logMsg;

    /**
     * Validates no pending rollback exists for the order.
     *
     * @param orderId order ID
     * @throws RollbackAlreadyPendingException if pending rollback exists
     */
    public void validateNoPendingRollback(Long orderId) {
        if (approvalRepository.existsByOrderIdAndApprovalStatus(orderId, ApprovalStatus.PENDING)) {
            log.warn(logMsg.get("rollback.log.already.pending", orderId));
            throw new RollbackAlreadyPendingException(orderId);
        }
    }

    /**
     * Validates order status allows rollback.
     *
     * @param salesOrder sales order entity
     * @throws RollbackFinalStatusException if status is final
     */
    public void validateRollbackPossibility(SalesOrder salesOrder) {
        OrderStatus status = salesOrder.getStatus();
        if (status == OrderStatus.COMPLETED ||
                status == OrderStatus.CANCELLED ||
                status == OrderStatus.REFUNDED ||
                status == OrderStatus.RETURNED) {
            log.warn(logMsg.get("rollback.error.final.status", status));
            throw new RollbackFinalStatusException(status);
        }
    }

    /**
     * Validates rollback approval is still pending.
     *
     * @param approval rollback approval entity
     * @throws RollbackAlreadyProcessedException if already processed
     */
    public void validateApprovalPending(RollbackApproval approval) {
        if (approval.getApprovalStatus() != ApprovalStatus.PENDING) {
            log.warn(logMsg.get("rollback.error.already.processed", approval.getId(), approval.getApprovalStatus()));
            throw new RollbackAlreadyProcessedException();
        }
    }
}