package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.OrderNotFoundException;
import ru.galtor85.household_store.advice.exception.RollbackApprovalNotFoundException;
import ru.galtor85.household_store.dto.RollbackApprovalDto;
import ru.galtor85.household_store.dto.RollbackRequest;
import ru.galtor85.household_store.entity.ApprovalStatus;
import ru.galtor85.household_store.entity.SalesOrder;
import ru.galtor85.household_store.entity.OrderStatus;
import ru.galtor85.household_store.entity.RollbackApproval;
import ru.galtor85.household_store.mapper.RollbackApprovalMapper;
import ru.galtor85.household_store.processor.*;
import ru.galtor85.household_store.repository.SalesOrderRepository;
import ru.galtor85.household_store.repository.RollbackApprovalRepository;
import ru.galtor85.household_store.validator.RollbackValidator;

@Slf4j
@Service
@RequiredArgsConstructor
public class RollbackService {

    private final RollbackApprovalRepository approvalRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final RollbackApprovalMapper approvalMapper;
    private final MessageService messageService;

    // Валидаторы
    private final RollbackValidator validator;

    // Процессоры
    private final RollbackTargetStatusProcessor targetStatusProcessor;
    private final RollbackRequestProcessor requestProcessor;
    private final RollbackDecisionProcessor decisionProcessor;
    private final RollbackExecutionProcessor executionProcessor;

    // ========== МЕНЕДЖЕР: запрос на откат ==========

    @Transactional
    public RollbackApprovalDto requestRollback(RollbackRequest request, Long managerId) {
        log.info(messageService.get("rollback.request.start", request.getOrderId(), managerId));

        // Проверяем существование заказа
        SalesOrder salesOrder = findOrderById(request.getOrderId());

        // Проверяем, нет ли уже активного запроса
        validator.validateNoPendingRollback(request.getOrderId());

        // Проверяем, можно ли откатить этот статус
        validator.validateRollbackPossibility(salesOrder);

        // Определяем целевой статус
        OrderStatus targetStatus = targetStatusProcessor.determineTargetStatus(salesOrder);

        // Создаем запрос
        RollbackApproval saved = requestProcessor.createRequest(salesOrder, request, managerId, targetStatus);

        log.info(messageService.get("rollback.request.complete", saved.getId(), request.getOrderId()));

        return approvalMapper.toDto(saved);
    }

    // ========== АДМИНИСТРАТОР: просмотр запросов ==========

    @Transactional(readOnly = true)
    public Page<RollbackApprovalDto> getPendingRollbacks(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("requestedAt").descending());

        Page<RollbackApproval> approvals = approvalRepository
                .findByApprovalStatus(ApprovalStatus.PENDING, pageable);

        log.debug(messageService.get("rollback.pending.fetched", approvals.getTotalElements()));

        return approvals.map(approvalMapper::toDto);
    }

    // ========== АДМИНИСТРАТОР: одобрение ==========

    @Transactional
    public RollbackApprovalDto approveRollback(Long approvalId, String adminComments, Long adminId) {
        log.info(messageService.get("rollback.approve.start", approvalId, adminId));

        // Находим запрос
        RollbackApproval approval = findApprovalById(approvalId);

        // Проверяем, что запрос еще в обработке
        validator.validateApprovalPending(approval);

        // Выполняем откат
        SalesOrder salesOrder = findOrderById(approval.getOrderId());
        executionProcessor.executeRollback(salesOrder, approval.getReason(), adminId);

        // Обновляем статус запроса
        RollbackApproval updated = decisionProcessor.approve(approval, adminComments, adminId);

        log.info(messageService.get("rollback.approve.complete", approvalId, salesOrder.getId()));

        return approvalMapper.toDto(updated);
    }

    // ========== АДМИНИСТРАТОР: отклонение ==========

    @Transactional
    public RollbackApprovalDto rejectRollback(Long approvalId, String adminComments, Long adminId) {
        log.info(messageService.get("rollback.reject.start", approvalId, adminId));

        // Находим запрос
        RollbackApproval approval = findApprovalById(approvalId);

        // Проверяем, что запрос еще в обработке
        validator.validateApprovalPending(approval);

        // Отклоняем запрос
        RollbackApproval updated = decisionProcessor.reject(approval, adminComments, adminId);

        log.info(messageService.get("rollback.reject.complete", approvalId));

        return approvalMapper.toDto(updated);
    }

    // ========== PRIVATE HELPER METHODS ==========

    private SalesOrder findOrderById(Long orderId) {
        return salesOrderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.order.log.not.found", orderId));
                    return new OrderNotFoundException(orderId);
                });
    }

    private RollbackApproval findApprovalById(Long approvalId) {
        return approvalRepository.findById(approvalId)
                .orElseThrow(() -> {
                    log.error(messageService.get("rollback.log.not.found", approvalId));
                    return new RollbackApprovalNotFoundException(approvalId);
                });
    }
}