package ru.galtor85.household_store.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for warehouse settings.
 *
 * <p>This class holds all warehouse-related configuration including default
 * warehouse ID, code, capacity, barcode format, and barcode prefix.</p>
 *
 * <p>All values are loaded from application.properties with prefix 'app.warehouse'.</p>
 *
 * <p><b>Example configuration in application.properties:</b></p>
 * <pre>
 * app.warehouse.default-id=1
 * app.warehouse.default-code=WH-DEFAULT
 * app.warehouse.default-capacity=1000
 * app.warehouse.default-barcode-format=CODE_128
 * app.warehouse.default-barcode-prefix=WH-DEFAULT-BARCODE-
 * </pre>
 *
 * @author G@LTor85
 * @since 1.0
 */
@Slf4j
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.warehouse")
public class WarehouseConfig {

    /** Default warehouse ID */
    private Long defaultId;

    /** Default warehouse code */
    private String defaultCode;

    /** Default warehouse capacity (number of storage cells) */
    private Integer defaultCapacity;

    /** Default barcode format for warehouse identification */
    private String defaultBarcodeFormat;

    /** Default barcode prefix for warehouse barcode generation */
    private String defaultBarcodePrefix;

    /**
     * Gets the default warehouse ID.
     *
     * @return default warehouse ID
     */
    public Long getDefaultWarehouseId() {
        return defaultId;
    }

    /**
     * Gets the default warehouse code.
     *
     * @return default warehouse code
     */
    public String getDefaultWarehouseCode() {
        return defaultCode;
    }

    /**
     * Gets the default warehouse capacity.
     * Returns 1000 as default value if not configured.
     *
     * @return default warehouse capacity (default: 1000)
     */
    public int getDefaultWarehouseCapacity() {
        return defaultCapacity != null ? defaultCapacity : 1000;
    }

    /**
     * Gets the default barcode format.
     * Returns "CODE_128" as default value if not configured.
     *
     * @return default barcode format (default: "CODE_128")
     */
    public String getDefaultWarehouseBarcodeFormat() {
        return defaultBarcodeFormat != null ? defaultBarcodeFormat : "CODE_128";
    }

    /**
     * Gets the default barcode prefix.
     * Returns "WH-DEFAULT-BARCODE-" as default value if not configured.
     *
     * @return default barcode prefix (default: "WH-DEFAULT-BARCODE-")
     */
    public String getDefaultWarehouseBarcodePrefix() {
        return defaultBarcodePrefix != null ? defaultBarcodePrefix : "WH-DEFAULT-BARCODE-";
    }
}