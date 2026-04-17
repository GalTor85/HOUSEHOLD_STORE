package ru.galtor85.household_store.entity.product;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a product in the system.
 *
 * <p>Contains all product information including pricing, inventory,
 * categorization, physical properties, storage requirements,
 * variants, attributes, and media.</p>
 *
 * @author G@LTor85
 
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@FieldNameConstants
@Table(name = "products", schema = "household_schema")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    private String category;

    private String brand;

    @Column(name = "image_url")
    private String imageUrl;

    /**
     * -- GETTER --
     *  Checks if the product is active.
     */
    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductAttribute> attributes = new ArrayList<>();

    @Column(name = "has_variants")
    private Boolean hasVariants;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_product_id")
    private Product parentProduct;

    @OneToMany(mappedBy = "parentProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Product> variants = new ArrayList<>();

    @Column(name = "barcode", unique = true)
    private String barcode;

    @Column(name = "barcode_format")
    private String barcodeFormat;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "supplier_price", precision = 10, scale = 2)
    private BigDecimal supplierPrice;

    @Column(name = "supplier_sku")
    private String supplierSku;

    @Column(name = "weight_kg")
    private Double weightKg;

    @Column(name = "volume_m3")
    private Double volumeM3;

    @Column(name = "requires_refrigeration")
    private Boolean requiresRefrigeration;

    @Column(name = "requires_freezing")
    private Boolean requiresFreezing;

    @Column(name = "is_hazardous")
    private Boolean isHazardous;

    @Column(name = "is_oversize")
    private Boolean isOversize;

    @Column(name = "is_liquid")
    private Boolean isLiquid;

    @Column(name = "is_palletized")
    private Boolean isPalletized;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "category_warehouse_id")
    private Long categoryWarehouseId;
}