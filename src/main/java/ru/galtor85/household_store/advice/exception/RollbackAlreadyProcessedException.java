package ru.galtor85.household_store.advice.exception;

public class RollbackAlreadyProcessedException extends RuntimeException {
    public RollbackAlreadyProcessedException() {
        super("error.rollback.already.processed");
    }
}