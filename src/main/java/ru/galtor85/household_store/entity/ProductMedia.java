package ru.galtor85.household_store.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product_media", schema = "household_schema",
        indexes = {
                @Index(name = "idx_product_media_product_id", columnList = "product_id"),
                @Index(name = "idx_product_media_type", columnList = "media_type"),
                @Index(name = "idx_product_media_is_main", columnList = "is_main")
        })
public class ProductMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId; // ID товара (связь по ID)

    @Column(name = "uploaded_by")
    private Long uploadedBy; // ID пользователя, загрузившего файл

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType;

    @Column(name = "file_name", nullable = false)
    private String fileName; // Имя файла

    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(name = "file_path", nullable = false)
    private String filePath; // Путь к файлу (URL)

    @Column(name = "file_size")
    private Long fileSize; // Размер в байтах

    @Column(name = "mime_type")
    private String mimeType; // MIME тип (image/jpeg, video/mp4)

    @Column(name = "alt_text")
    private String altText; // Альтернативный текст (для SEO)

    @Column(name = "caption")
    private String caption; // Подпись к изображению

    @Column(name = "sort_order")
    private Integer sortOrder; // Порядок сортировки

    @Column(name = "is_main")
    private Boolean isMain; // Главное изображение

    @Column(name = "width")
    private Integer width; // Ширина (для изображений)

    @Column(name = "height")
    private Integer height; // Высота (для изображений)

    @Column(name = "duration")
    private Integer duration; // Длительность видео (в секундах)

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}