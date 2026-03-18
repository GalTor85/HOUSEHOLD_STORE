package ru.galtor85.household_store.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class WarehouseConfig {

    @Value("${app.warehouse.default-id:1}")
    private Long defaultWarehouseId;

    @Value("${app.warehouse.default-name:Main Warehouse}")
    private String defaultWarehouseName;
}