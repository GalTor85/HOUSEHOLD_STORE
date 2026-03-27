package ru.galtor85.household_store.advice.exception.cell;

public class CellVolumeLimitExceededException extends RuntimeException {
    private final Long cellId;
    private final String cellCode;
    private final double maxVolume;
    private final double requestedVolume;

    public CellVolumeLimitExceededException(Long cellId, double maxVolume, double requestedVolume) {
        super();
        this.cellId = cellId;
        this.cellCode = null;
        this.maxVolume = maxVolume;
        this.requestedVolume = requestedVolume;
    }

    public Long getCellId() {
        return cellId;
    }

    public String getCellCode() {
        return cellCode;
    }

    public double getMaxVolume() {
        return maxVolume;
    }

    public double getRequestedVolume() {
        return requestedVolume;
    }
}