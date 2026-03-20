package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.dto.RollbackRequest;
import ru.galtor85.household_store.entity.ApprovalStatus;
import ru.galtor85.household_store.entity.Order;
import ru.galtor85.household_store.entity.OrderStatus;
import ru.galtor85.household_store.entity.RollbackApproval;
import ru.galtor85.household_store.repository.RollbackApprovalRepository;
import ru.galtor85.household_store.service.MessageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class RollbackRequestProcessor {

    private final RollbackApprovalRepository approvalRepository;
    private final RollbackTargetStatusProcessor targetStatusProcessor;
    private final MessageService messageService;

    @Transactional
    public RollbackApproval createRequest(Order order, RollbackRequest request,
                                          Long managerId, OrderStatus targetStatus) {

        log.debug(messageService.get("rollback.request.creating",
                order.getId(), managerId, targetStatus));

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

        log.info(messageService.get("rollback.request.created",
                saved.getId(), order.getId(), managerId));

        return saved;
    }
}