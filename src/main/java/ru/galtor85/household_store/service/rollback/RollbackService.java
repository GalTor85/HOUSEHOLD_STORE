package ru.galtor85.household_store.service.rollback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.order.OrderNotFoundException;
import ru.galtor85.household_store.advice.exception.rollback.RollbackApprovalNotFoundException;
import ru.galtor85.household_store.dto.request.order.RollbackRequest;
import ru.galtor85.household_store.dto.response.order.RollbackApprovalDto;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.entity.rollback.ApprovalStatus;
import ru.galtor85.household_store.entity.rollback.RollbackApproval;
import ru.galtor85.household_store.mapper.rollback.RollbackApprovalMapper;
import ru.galtor85.household_store.processor.rollback.RollbackDecisionProcessor;
import ru.galtor85.household_store.processor.rollback.RollbackExecutionProcessor;
import ru.galtor85.household_store.processor.rollback.RollbackRequestProcessor;
import ru.galtor85.household_store.processor.rollback.RollbackTargetStatusProcessor;
import ru.galtor85.household_store.repository.order.SalesOrderRepository;
import ru.galtor85.household_store.repository.rollback.RollbackApprovalRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.validator.rollback.RollbackValidator;

/**
 * Service for managing order rollback requests.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RollbackService {

    private final RollbackApprovalRepository approvalRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final RollbackApprovalMapper approvalMapper;
    private final LogMessageService logMsg;
    private final RollbackValidator validator;
    private final RollbackTargetStatusProcessor targetStatusProcessor;
    private final RollbackRequestProcessor requestProcessor;
    private final RollbackDecisionProcessor decisionProcessor;
    private final RollbackExecutionProcessor executionProcessor;

    /**
     * Requests a rollback for an order.
     *
     * @param request rollback request
     * @param managerId manager ID
     * @return created rollback approval DTO
     */
    @Transactional
    public RollbackApprovalDto requestRollback(RollbackRequest request, Long managerId) {
        log.info(logMsg.get("rollback.request.start", request.getOrderId(), managerId));

        SalesOrder salesOrder = findOrderById(request.getOrderId());
        validator.validateNoPendingRollback(request.getOrderId());
        validator.validateRollbackPossibility(salesOrder);

        OrderStatus targetStatus = targetStatusProcessor.determineTargetStatus(salesOrder);
        RollbackApproval saved = requestProcessor.createRequest(salesOrder, request, managerId, targetStatus);

        log.info(logMsg.get("rollback.request.complete", saved.getId(), request.getOrderId()));

        return approvalMapper.toDto(saved);
    }

    /**
     * Gets paginated list of pending rollback requests.
     *
     * @param page page number
     * @param size page size
     * @return page of rollback approval DTOs
     */
    @Transactional(readOnly = true)
    public Page<RollbackApprovalDto> getPendingRollbacks(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("requestedAt").descending());
        Page<RollbackApproval> approvals = approvalRepository
                .findByApprovalStatus(ApprovalStatus.PENDING, pageable);

        log.debug(logMsg.get("rollback.pending.fetched", approvals.getTotalElements()));

        return approvals.map(approvalMapper::toDto);
    }

    /**
     * Approves a rollback request.
     *
     * @param approvalId approval ID
     * @param adminComments admin comments
     * @param adminId admin ID
     * @return updated rollback approval DTO
     */
    @Transactional
    public RollbackApprovalDto approveRollback(Long approvalId, String adminComments, Long adminId) {
        log.info(logMsg.get("rollback.approve.start", approvalId, adminId));

        RollbackApproval approval = findApprovalById(approvalId);
        validator.validateApprovalPending(approval);

        SalesOrder salesOrder = findOrderById(approval.getOrderId());
        executionProcessor.executeRollback(salesOrder, approval.getReason(), adminId);

        RollbackApproval updated = decisionProcessor.approve(approval, adminComments, adminId);

        log.info(logMsg.get("rollback.approve.complete", approvalId, salesOrder.getId()));

        return approvalMapper.toDto(updated);
    }

    /**
     * Rejects a rollback request.
     *
     * @param approvalId approval ID
     * @param adminComments admin comments
     * @param adminId admin ID
     * @return updated rollback approval DTO
     */
    @Transactional
    public RollbackApprovalDto rejectRollback(Long approvalId, String adminComments, Long adminId) {
        log.info(logMsg.get("rollback.reject.start", approvalId, adminId));

        RollbackApproval approval = findApprovalById(approvalId);
        validator.validateApprovalPending(approval);

        RollbackApproval updated = decisionProcessor.reject(approval, adminComments, adminId);

        log.info(logMsg.get("rollback.reject.complete", approvalId));

        return approvalMapper.toDto(updated);
    }

    private SalesOrder findOrderById(Long orderId) {
        return salesOrderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error(logMsg.get("manager.order.log.not.found", orderId));
                    return new OrderNotFoundException(orderId);
                });
    }

    private RollbackApproval findApprovalById(Long approvalId) {
        return approvalRepository.findById(approvalId)
                .orElseThrow(() -> {
                    log.error(logMsg.get("rollback.log.not.found", approvalId));
                    return new RollbackApprovalNotFoundException(approvalId);
                });
    }
}