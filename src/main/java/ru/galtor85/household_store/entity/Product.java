package ru.galtor85.household_store.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
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

    //  вариативные характеристики
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductAttribute> attributes = new ArrayList<>();

    //  флаг, что товар имеет варианты (например, разные цвета/размеры)
    @Column(name = "has_variants")
    private Boolean hasVariants;

    // ссылка на родительский товар (для вариантов)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_product_id")
    private Product parentProduct;

    // список дочерних вариантов
    @OneToMany(mappedBy = "parentProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Product> variants = new ArrayList<>();

    // ШТРИХ КОД
    @Column(name = "barcode", unique = true)
    private String barcode; // Штрих-код (EAN-13, UPC)

    @Column(name = "barcode_format")
    private String barcodeFormat; // EAN_13, UPC_A, QR_CODE, etc.

    @Column(name = "supplier_id")
    private Long supplierId; // Основной поставщик

    @Column(name = "supplier_price", precision = 10, scale = 2)
    private BigDecimal supplierPrice; // Закупочная цена

    @Column(name = "supplier_sku")
    private String supplierSku; // Артикул поставщика

    @Column(name = "weight_kg")
    private Double weightKg;           // Вес в кг

    @Column(name = "volume_m3")
    private Double volumeM3;           // Объем в м³

    @Column(name = "requires_refrigeration")
    private Boolean requiresRefrigeration;  // Требует охлаждения

    @Column(name = "requires_freezing")
    private Boolean requiresFreezing;       // Требует заморозки

    @Column(name = "is_hazardous")
    private Boolean isHazardous;            // Опасный груз

    @Column(name = "is_oversize")
    private Boolean isOversize;              // Негабарит

    @Column(name = "is_liquid")
    private Boolean isLiquid;                // Жидкость

    @Column(name = "is_palletized")
    private Boolean isPalletized;            // Паллетированный груз

    @Column(name = "warehouse_id")
    private Long warehouseId; // Прямое назначение склада (приоритет 1)

    @Column(name = "category_warehouse_id")
    private Long categoryWarehouseId; // Склад из категории (приоритет 2)

    // Поле для детального учета по складам
    @OneToMany(mappedBy = "productId", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductStock> stocks = new ArrayList<>();


    // Вспомогательные методы
    public void addAttribute(ProductAttribute attribute) {
        attributes.add(attribute);
        attribute.setProduct(this);
    }

    public void removeAttribute(ProductAttribute attribute) {
        attributes.remove(attribute);
        attribute.setProduct(null);
    }

    public void addVariant(Product variant) {
        variants.add(variant);
        variant.setParentProduct(this);
    }

    public void removeVariant(Product variant) {
        variants.remove(variant);
        variant.setParentProduct(null);
    }

    // Медиа

    @OneToMany(mappedBy = "productId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductMedia> media = new ArrayList<>();

    // Вспомогательные методы
    public void addMedia(ProductMedia mediaItem) {
        media.add(mediaItem);
        mediaItem.setProductId(this.id);
    }

    public void removeMedia(ProductMedia mediaItem) {
        media.remove(mediaItem);
        mediaItem.setProductId(null);
    }

    public List<ProductMedia> getMainImages() {
        return media.stream()
                .filter(m -> m.getMediaType() == MediaType.IMAGE && m.getIsMain())
                .collect(Collectors.toList());
    }

    public List<ProductMedia> getAllImages() {
        return media.stream()
                .filter(m -> m.getMediaType() == MediaType.IMAGE)
                .sorted(Comparator.comparing(ProductMedia::getSortOrder))
                .collect(Collectors.toList());
    }

    public List<ProductMedia> getVideos() {
        return media.stream()
                .filter(m -> m.getMediaType() == MediaType.VIDEO)
                .collect(Collectors.toList());
    }

}