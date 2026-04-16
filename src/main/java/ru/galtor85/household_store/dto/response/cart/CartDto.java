package ru.galtor85.household_store.dto.response.cart;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.cart.CartStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Cart DTO", title = "Cart")
public class CartDto {

    @Schema(description = "Cart ID", example = "1")
    private Long id;

    @Schema(description = "User ID", example = "1")
    private Long userId;

    @Schema(description = "Cart status")
    private CartStatus status;

    @Schema(description = "Localized status", example = "Active")
    private String localizedStatus;

    @Schema(description = "Cart category", example = "Electronics")
    private String category;

    @Schema(description = "Cart items")
    private List<CartItemDto> items;

    @Schema(description = "Total amount", example = "1999.98")
    private BigDecimal totalAmount;

    @Schema(description = "Items count", example = "3")
    private Integer itemsCount;

    @Schema(description = "Created at")
    private LocalDateTime createdAt;

    @Schema(description = "Updated at")
    private LocalDateTime updatedAt;

    @Schema(description = "Expires at")
    private LocalDateTime expiresAt;
}