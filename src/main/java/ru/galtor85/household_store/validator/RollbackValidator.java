package ru.galtor85.household_store.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.RollbackAlreadyPendingException;
import ru.galtor85.household_store.advice.exception.RollbackAlreadyProcessedException;
import ru.galtor85.household_store.advice.exception.RollbackFinalStatusException;
import ru.galtor85.household_store.entity.ApprovalStatus;
import ru.galtor85.household_store.entity.Order;
import ru.galtor85.household_store.entity.OrderStatus;
import ru.galtor85.household_store.entity.RollbackApproval;
import ru.galtor85.household_store.repository.RollbackApprovalRepository;
import ru.galtor85.household_store.service.MessageService;

import java.util.Locale;

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

    public void validateRollbackPossibility(Order order) {
        OrderStatus status = order.getStatus();

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