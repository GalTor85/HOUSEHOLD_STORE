package ru.galtor85.household_store.advice.exception.cell;

import lombok.Getter;

@Getter
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

}