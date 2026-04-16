package ru.galtor85.household_store.repository.rollback;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.rollback.ApprovalStatus;
import ru.galtor85.household_store.entity.rollback.RollbackApproval;

/**
 * Repository for rollback approval operations.
 */
@Repository
public interface RollbackApprovalRepository extends JpaRepository<RollbackApproval, Long> {

    /**
     * Finds rollback approvals by status with pagination.
     *
     * @param status approval status
     * @param pageable pagination information
     * @return page of rollback approvals
     */
    Page<RollbackApproval> findByApprovalStatus(ApprovalStatus status, Pageable pageable);

    /**
     * Checks if a pending rollback exists for an order.
     *
     * @param orderId order ID
     * @param status approval status
     * @return true if exists
     */
    boolean existsByOrderIdAndApprovalStatus(Long orderId, ApprovalStatus status);
}