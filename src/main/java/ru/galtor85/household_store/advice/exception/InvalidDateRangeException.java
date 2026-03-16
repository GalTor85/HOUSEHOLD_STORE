package ru.galtor85.household_store.advice.exception;

public class InvalidDateRangeException extends RuntimeException {
    private final String startDate;
    private final String endDate;

    public InvalidDateRangeException(String startDate, String endDate) {
        super();
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }
}