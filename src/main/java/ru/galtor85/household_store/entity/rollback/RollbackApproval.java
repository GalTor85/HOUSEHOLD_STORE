package ru.galtor85.household_store.entity.rollback;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rollback_approvals", schema = "household_schema")
public class RollbackApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "current_status", nullable = false)
    private String currentStatus;

    @Column(name = "target_status", nullable = false)
    private String targetStatus;

    @Column(name = "requested_by", nullable = false)
    private Long requestedById;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "comments", length = 1000)
    private String comments;

    @Column(name = "approval_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ApprovalStatus approvalStatus;

    @Column(name = "reviewed_by")
    private Long reviewedById;

    @Column(name = "admin_comments", length = 500)
    private String adminComments;

    @CreationTimestamp
    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}