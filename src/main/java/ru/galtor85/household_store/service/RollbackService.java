package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.*;
import ru.galtor85.household_store.dto.RollbackApprovalDto;
import ru.galtor85.household_store.dto.RollbackRequest;
import ru.galtor85.household_store.entity.*;
import ru.galtor85.household_store.mapper.RollbackApprovalMapper;
import ru.galtor85.household_store.repository.OrderRepository;
import ru.galtor85.household_store.repository.RollbackApprovalRepository;
import ru.galtor85.household_store.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class RollbackService {

    private final RollbackApprovalRepository approvalRepository;
    private final OrderRepository orderRepository;
    private final ManagerOrderService orderService;
    private final RollbackApprovalMapper approvalMapper;
    private final MessageService messageService;


    // ========== МЕНЕДЖЕР: запрос на откат ==========

    @Transactional
    public RollbackApprovalDto requestRollback(RollbackRequest request, Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        // Проверяем существование заказа
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.order.log.not.found", request.getOrderId()));
                    return new OrderNotFoundException(request.getOrderId());
                });

        // Проверяем, нет ли уже активного запроса
        if (approvalRepository.existsByOrderIdAndApprovalStatus(
                request.getOrderId(), ApprovalStatus.PENDING)) {
            log.warn(messageService.get("rollback.log.already.pending", request.getOrderId()));
            throw new RollbackAlreadyPendingException(request.getOrderId());
        }

        // Проверяем, можно ли откатить этот статус
        validateRollbackPossibility(order, locale);

        // Определяем целевой статус
        OrderStatus targetStatus = determineTargetStatus(order);

        // Создаем запрос
        RollbackApproval approval = RollbackApproval.builder()
                .orderId(request.getOrderId())
                .currentStatus(order.getStatus().name())
                .targetStatus(targetStatus.name())
                .requestedById(managerId)
                .reason(request.getReason())
                .comments(request.getComments())
                .approvalStatus(ApprovalStatus.PENDING)
                .build();

        RollbackApproval saved = approvalRepository.save(approval);

        log.info(messageService.get("rollback.log.requested",
                request.getOrderId(), managerId));

        return approvalMapper.toDto(saved, locale);
    }

    // ========== АДМИНИСТРАТОР: просмотр запросов ==========

    @Transactional(readOnly = true)
    public Page<RollbackApprovalDto> getPendingRollbacks(int page, int size, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Pageable pageable = PageRequest.of(page, size, Sort.by("requestedAt").descending());

        Page<RollbackApproval> approvals = approvalRepository
                .findByApprovalStatus(ApprovalStatus.PENDING, pageable);

        Locale finalLocale = locale;
        return approvals.map(approval -> approvalMapper.toDto(approval, finalLocale));
    }

    // ========== АДМИНИСТРАТОР: одобрение/отклонение ==========

    @Transactional
    public RollbackApprovalDto approveRollback(Long approvalId, String adminComments,
                                               Long adminId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        RollbackApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> {
                    log.error(messageService.get("rollback.log.not.found", approvalId));
                    return new RollbackApprovalNotFoundException(approvalId);
                });

        if (approval.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException(
                    messageService.get("rollback.error.already.processed")
            );
        }

        // Выполняем откат через существующий сервис
        Order order = orderRepository.findById(approval.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException(approval.getOrderId()));

        try {
            // Вызываем метод отката из ManagerOrderService
            orderService.rollbackOrderStatus(
                    approval.getOrderId(),
                    approval.getReason(),
                    adminId,
                    locale
            );

            // Обновляем статус запроса
            approval.setApprovalStatus(ApprovalStatus.APPROVED);
            approval.setReviewedById(adminId);
            approval.setAdminComments(adminComments);
            approval.setReviewedAt(LocalDateTime.now());

            log.info(messageService.get("rollback.log.approved",
                    approval.getOrderId(), adminId));

        } catch (Exception e) {
            log.error(messageService.get("rollback.log.approval.failed",
                    approval.getOrderId(), e.getMessage()));
            throw new RollbackExecutionException(approval.getOrderId(), e.getMessage());
        }

        RollbackApproval updated = approvalRepository.save(approval);
        return approvalMapper.toDto(updated, locale);
    }

    @Transactional
    public RollbackApprovalDto rejectRollback(Long approvalId, String adminComments,
                                              Long adminId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        RollbackApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> {
                    log.error(messageService.get("rollback.log.not.found", approvalId));
                    return new RollbackApprovalNotFoundException(approvalId);
                });

        if (approval.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException(
                    messageService.get("rollback.error.already.processed")
            );
        }

        approval.setApprovalStatus(ApprovalStatus.REJECTED);
        approval.setReviewedById(adminId);
        approval.setAdminComments(adminComments);
        approval.setReviewedAt(LocalDateTime.now());

        RollbackApproval updated = approvalRepository.save(approval);

        log.info(messageService.get("rollback.log.rejected",
                approval.getOrderId(), adminId));

        return approvalMapper.toDto(updated, locale);
    }

    // ========== PRIVATE METHODS ==========

    private void validateRollbackPossibility(Order order, Locale locale) {
        OrderStatus status = order.getStatus();

        if (status == OrderStatus.COMPLETED ||
                status == OrderStatus.CANCELLED ||
                status == OrderStatus.REFUNDED ||
                status == OrderStatus.RETURNED) {

            throw new IllegalStateException(
                    messageService.get("rollback.error.final.status", status)
            );
        }
    }

    private OrderStatus determineTargetStatus(Order order) {
        return switch (order.getStatus()) {
            case PAID -> OrderStatus.PENDING;
            case PROCESSING -> OrderStatus.PAID;
            case SHIPPED -> OrderStatus.PROCESSING;
            case DELIVERED -> OrderStatus.SHIPPED;
            default -> throw new IllegalStateException(
                    "Cannot determine target status for " + order.getStatus()
            );
        };
    }
}