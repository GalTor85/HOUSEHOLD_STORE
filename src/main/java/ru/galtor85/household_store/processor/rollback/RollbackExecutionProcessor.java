package ru.galtor85.household_store.processor.rollback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.rollback.RollbackExecutionException;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.order.SalesOrderService;

/**
 * Processor for executing rollback operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RollbackExecutionProcessor {

    private final SalesOrderService orderService;
    private final LogMessageService logMsg;

    /**
     * Executes a rollback for a sales order.
     *
     * @param salesOrder the sales order to rollback
     * @param reason     the rollback reason
     * @param adminId    the admin ID performing the rollback
     * @throws RollbackExecutionException if execution fails
     */
    @Transactional
    public void executeRollback(SalesOrder salesOrder, String reason, Long adminId) {
        try {
            log.info(logMsg.get("rollback.execution.start",
                    salesOrder.getId(), adminId, reason));

            orderService.rollbackOrderStatus(salesOrder.getId(), reason, adminId);

            log.info(logMsg.get("rollback.execution.success",
                    salesOrder.getId(), adminId));

        } catch (Exception e) {
            log.error(logMsg.get("rollback.execution.error",
                    salesOrder.getId(), e.getMessage()), e);
            throw new RollbackExecutionException(salesOrder.getId(), e.getMessage());
        }
    }
}