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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Entity representing a product in the system.
 *
 * <p>Contains all product information including pricing, inventory,
 * categorization, physical properties, storage requirements,
 * variants, attributes, and media.</p>
 *
 * @author G@LTor85
 * @since 1.0
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

    @Column(name = "quantity_in_stock")
    private Integer quantityInStock;

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

    @OneToMany(mappedBy = "productId", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductStock> stocks = new ArrayList<>();

    @OneToMany(mappedBy = "productId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductMedia> media = new ArrayList<>();

    /**
     * Adds an attribute to the product.
     *
     * @param attribute the attribute to add
     */
    public void addAttribute(ProductAttribute attribute) {
        attributes.add(attribute);
        attribute.setProduct(this);
    }

    /**
     * Removes an attribute from the product.
     *
     * @param attribute the attribute to remove
     */
    public void removeAttribute(ProductAttribute attribute) {
        attributes.remove(attribute);
        attribute.setProduct(null);
    }

    /**
     * Adds a variant to the product.
     *
     * @param variant the variant to add
     */
    public void addVariant(Product variant) {
        variants.add(variant);
        variant.setParentProduct(this);
    }

    /**
     * Removes a variant from the product.
     *
     * @param variant the variant to remove
     */
    public void removeVariant(Product variant) {
        variants.remove(variant);
        variant.setParentProduct(null);
    }

    /**
     * Adds media to the product.
     *
     * @param mediaItem the media item to add
     */
    public void addMedia(ProductMedia mediaItem) {
        media.add(mediaItem);
        mediaItem.setProductId(this.id);
    }

    /**
     * Removes media from the product.
     *
     * @param mediaItem the media item to remove
     */
    public void removeMedia(ProductMedia mediaItem) {
        media.remove(mediaItem);
        mediaItem.setProductId(null);
    }

    /**
     * Gets all main images for the product.
     *
     * @return list of main product images
     */
    public List<ProductMedia> getMainImages() {
        return media.stream()
                .filter(m -> m.getMediaType() == MediaType.IMAGE && Boolean.TRUE.equals(m.getIsMain()))
                .collect(Collectors.toList());
    }

    /**
     * Gets all images for the product sorted by sort order.
     *
     * @return list of all product images
     */
    public List<ProductMedia> getAllImages() {
        return media.stream()
                .filter(m -> m.getMediaType() == MediaType.IMAGE)
                .sorted(Comparator.comparing(ProductMedia::getSortOrder))
                .collect(Collectors.toList());
    }

    /**
     * Gets all videos for the product.
     *
     * @return list of product videos
     */
    public List<ProductMedia> getVideos() {
        return media.stream()
                .filter(m -> m.getMediaType() == MediaType.VIDEO)
                .collect(Collectors.toList());
    }

    /**
     * Checks if the product has variants.
     *
     * @return true if product has variants
     */
    public boolean hasVariants() {
        return Boolean.TRUE.equals(hasVariants);
    }
}