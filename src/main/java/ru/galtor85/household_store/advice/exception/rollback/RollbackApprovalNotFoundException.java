package ru.galtor85.household_store.advice.exception.rollback;

import lombok.Getter;

@Getter
public class RollbackApprovalNotFoundException extends RuntimeException {
    private final Long approvalId;

    public RollbackApprovalNotFoundException(Long approvalId) {
        super("error.rollback.approval.not.found");
        this.approvalId = approvalId;
    }
}