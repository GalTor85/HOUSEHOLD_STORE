package ru.galtor85.household_store.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.ApprovalStatus;
import ru.galtor85.household_store.entity.RollbackApproval;

import java.util.List;
import java.util.Optional;

@Repository
public interface RollbackApprovalRepository extends JpaRepository<RollbackApproval, Long> {

    List<RollbackApproval> findByOrderId(Long orderId);

    Page<RollbackApproval> findByApprovalStatus(ApprovalStatus status, Pageable pageable);

    Optional<RollbackApproval> findByOrderIdAndApprovalStatus(Long orderId, ApprovalStatus status);

    @Query("SELECT ra FROM RollbackApproval ra WHERE ra.requestedById = :managerId ORDER BY ra.requestedAt DESC")
    List<RollbackApproval> findByRequestedById(@Param("managerId") Long managerId);

    boolean existsByOrderIdAndApprovalStatus(Long orderId, ApprovalStatus status);
}