package ru.galtor85.household_store.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.currency")
public class CurrencyConfig {

    private String defaultCode;
    private BigDecimal defaultExchangeRate;
    private Integer defaultDecimalPlaces;
    private boolean defaultActive;
    private boolean defaultBase;
}