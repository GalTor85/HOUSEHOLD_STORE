package ru.galtor85.household_store.processor.warehouse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.calculator.StockCalculator;
import ru.galtor85.household_store.dto.response.stock.TopProductDto;
import ru.galtor85.household_store.dto.response.warehouse.WarehouseStockSummaryDto;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.product.ProductStock;
import ru.galtor85.household_store.entity.warehouse.Warehouse;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Processor for building warehouse stock summaries.
 *
 * <p>Aggregates stock data for a warehouse into a comprehensive summary
 * including totals, low stock counts, utilization percentage, and
 * top products by value.</p>
 *
 * @author G@LTor85
 
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseSummaryProcessor {

    private static final int TOP_PRODUCTS_LIMIT = 5;
    private static final String UNKNOWN_PRODUCT_NAME_KEY = "stock.product.unknown";
    private static final double DEFAULT_VALUE = 0.0;

    private final ProductRepository productRepository;
    private final StockCalculator stockCalculator;
    private final MessageService messageService;

    /**
     * Builds a comprehensive stock summary for a warehouse.
     *
     * <p>The summary includes:
     * <ul>
     *   <li>Total number of distinct products</li>
     *   <li>Total quantity of all items</li>
     *   <li>Total stock value</li>
     *   <li>Count of low stock items</li>
     *   <li>Count of out of stock items</li>
     *   <li>Warehouse utilization percentage</li>
     *   <li>Top 5 products by value</li>
     * </ul>
     *
     * @param warehouse the warehouse entity
     * @param stocks list of stock records for the warehouse
     * @return DTO containing the complete warehouse stock summary
     */
    public WarehouseStockSummaryDto buildSummary(Warehouse warehouse, List<ProductStock> stocks) {
        int totalProducts = stocks.size();
        int totalItems = stockCalculator.sumQuantity(stocks);
        double totalValue = stockCalculator.calculateTotalValue();
        int lowStockCount = stockCalculator.countLowStock(stocks);
        int outOfStockCount = stockCalculator.countOutOfStock(stocks);
        double utilizationPercentage = stockCalculator.calculateUtilization(warehouse, stocks);

        List<TopProductDto> topProducts = buildTopProducts(stocks);

        return WarehouseStockSummaryDto.builder()
                .warehouseId(warehouse.getId())
                .warehouseName(warehouse.getName())
                .totalProducts(totalProducts)
                .totalItems(totalItems)
                .totalValue(totalValue)
                .lowStockCount(lowStockCount)
                .outOfStockCount(outOfStockCount)
                .utilizationPercentage(utilizationPercentage)
                .topProducts(topProducts)
                .build();
    }

    /**
     * Builds the list of top products by value.
     *
     * <p>Products are sorted by total value (price × quantity) in descending order,
     * and limited to the top {@value #TOP_PRODUCTS_LIMIT} items.</p>
     *
     * @param stocks list of stock records
     * @return list of top products sorted by value descending
     */
    private List<TopProductDto> buildTopProducts(List<ProductStock> stocks) {
        return stocks.stream()
                .map(this::createTopProductDto)
                .sorted(Comparator.comparing(TopProductDto::getValue).reversed())
                .limit(TOP_PRODUCTS_LIMIT)
                .collect(Collectors.toList());
    }

    /**
     * Creates a TopProductDto from a ProductStock record.
     *
     * <p>Calculates the total value as price × quantity.
     * If the product is not found, uses a localized "Unknown" name.</p>
     *
     * @param stock the stock record
     * @return DTO containing product information and calculated value
     */
    private TopProductDto createTopProductDto(ProductStock stock) {
        Product product = productRepository.findById(stock.getProductId()).orElse(null);

        String productName = resolveProductName(product);
        double value = calculateStockValue(product, stock.getQuantity());

        return new TopProductDto(stock.getProductId(), productName, stock.getQuantity(), value);
    }

    /**
     * Resolves the product name, falling back to localized "Unknown" if product not found.
     *
     * @param product the product entity (maybe null)
     * @return product name or localized fallback
     */
    private String resolveProductName(Product product) {
        if (product != null && product.getName() != null) {
            return product.getName();
        }
        return messageService.get(UNKNOWN_PRODUCT_NAME_KEY);
    }

    /**
     * Calculates the total value of stock for a product.
     *
     * @param product the product entity (maybe null)
     * @param quantity the quantity in stock
     * @return calculated stock value or 0 if product/price is null
     */
    private double calculateStockValue(Product product, int quantity) {
        if (product != null && product.getPrice() != null) {
            return product.getPrice().doubleValue() * quantity;
        }
        return DEFAULT_VALUE;
    }
}