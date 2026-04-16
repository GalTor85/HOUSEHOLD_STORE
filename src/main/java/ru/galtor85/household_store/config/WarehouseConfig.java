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
 * warehouse ID, code, name, description, address, capacity, barcode format,
 * and barcode prefix.</p>
 *
 * <p>All values are loaded from application.properties with prefix 'app.warehouse'.</p>
 *
 * <p><b>Example configuration in application.properties:</b></p>
 * <pre>
 * app.warehouse.default-id=1
 * app.warehouse.default-code=WH-DEFAULT
 * app.warehouse.default-name=Main Warehouse
 * app.warehouse.default-description=Default warehouse for the system
 * app.warehouse.default-address=System Default Address
 * app.warehouse.default-capacity=1000
 * app.warehouse.default-barcode-format=CODE_128
 * app.warehouse.default-barcode-prefix=WH-DEFAULT-BARCODE-
 * app.warehouse.default-visible-for-sale=true
 * </pre>
 *
 * @author G@LTor85
 
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

    /** Default warehouse name */
    private String defaultName;

    /** Default warehouse description */
    private String defaultDescription;

    /** Default warehouse address */
    private String defaultAddress;

    /** Default warehouse capacity (number of storage cells) */
    private Integer defaultCapacity;

    /** Default barcode format for warehouse identification */
    private String defaultBarcodeFormat;

    /** Default barcode prefix for warehouse barcode generation */
    private String defaultBarcodePrefix;

    /** Default visibility for sale */
    private Boolean defaultVisibleForSale;

    /**
     * Gets the default warehouse ID.
     *
     * @return default warehouse ID
     */
    public Long getDefaultWarehouseId() {
        return defaultId != null ? defaultId : 1L;
    }

    /**
     * Gets the default warehouse code.
     *
     * @return default warehouse code
     */
    public String getDefaultWarehouseCode() {
        return defaultCode != null ? defaultCode : "WH-DEFAULT";
    }

    /**
     * Gets the default warehouse name.
     *
     * @return default warehouse name
     */
    public String getDefaultWarehouseName() {
        return defaultName != null ? defaultName : "Main Warehouse";
    }

    /**
     * Gets the default warehouse description.
     *
     * @return default warehouse description
     */
    public String getDefaultWarehouseDescription() {
        return defaultDescription != null ? defaultDescription : "Default warehouse for the system";
    }

    /**
     * Gets the default warehouse address.
     *
     * @return default warehouse address
     */
    public String getDefaultWarehouseAddress() {
        return defaultAddress != null ? defaultAddress : "System Default Address";
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

    /**
     * Gets the default visibility for sale.
     * Returns true as default value if not configured.
     *
     * @return default visibility for sale (default: true)
     */
    public boolean getDefaultWarehouseVisibleForSale() {
        return defaultVisibleForSale != null ? defaultVisibleForSale : true;
    }
}