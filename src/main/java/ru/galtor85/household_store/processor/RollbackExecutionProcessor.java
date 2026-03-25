package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.RollbackExecutionException;
import ru.galtor85.household_store.entity.SalesOrder;
import ru.galtor85.household_store.service.ManagerSalesOrderService;
import ru.galtor85.household_store.service.MessageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class RollbackExecutionProcessor {

    private final ManagerSalesOrderService orderService;
    private final MessageService messageService;

    @Transactional
    public void executeRollback(SalesOrder salesOrder, String reason, Long adminId) {
        try {
            log.info(messageService.get("rollback.execution.start",
                    salesOrder.getId(), adminId, reason));

            orderService.rollbackOrderStatus(salesOrder.getId(), reason, adminId);

            log.info(messageService.get("rollback.execution.success",
                    salesOrder.getId(), adminId));

        } catch (Exception e) {
            log.error(messageService.get("rollback.execution.error",
                    salesOrder.getId(), e.getMessage()), e);
            throw new RollbackExecutionException(salesOrder.getId(), e.getMessage());
        }
    }
}