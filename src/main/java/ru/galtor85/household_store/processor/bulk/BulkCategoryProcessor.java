package ru.galtor85.household_store.processor.bulk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.dto.request.warehouse.BulkCategoryWarehouseRequest;
import ru.galtor85.household_store.dto.response.warehouse.CategoryWarehouseDto;
import ru.galtor85.household_store.entity.warehouse.CategoryWarehouse;
import ru.galtor85.household_store.entity.warehouse.Warehouse;
import ru.galtor85.household_store.mapper.category.CategoryWarehouseMapper;
import ru.galtor85.household_store.repository.category.CategoryWarehouseRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;

import java.util.ArrayList;
import java.util.List;

/**
 * Processor for bulk category-warehouse assignments.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BulkCategoryProcessor {

    private final CategoryWarehouseRepository categoryWarehouseRepository;
    private final CategoryWarehouseMapper mapper;
    private final LogMessageService logMsg;

    /**
     * Processes bulk assignment of categories to a warehouse.
     *
     * @param request   the bulk assignment request
     * @param warehouse the target warehouse
     * @param createdBy ID of the user performing the operation
     * @return BulkAssignmentResult containing successful and skipped assignments
     */
    @Transactional
    public BulkAssignmentResult processBulkAssign(BulkCategoryWarehouseRequest request,
                                                  Warehouse warehouse,
                                                  Long createdBy) {

        List<CategoryWarehouseDto> results = new ArrayList<>();
        List<String> alreadyAssigned = new ArrayList<>();
        List<String> newlyAssigned = new ArrayList<>();

        for (String category : request.getCategories()) {
            if (categoryWarehouseRepository.findByCategory(category).isPresent()) {
                alreadyAssigned.add(category);
                continue;
            }

            CategoryWarehouse assignment = CategoryWarehouse.builder()
                    .category(category)
                    .warehouseId(request.getWarehouseId())
                    .isDefault(request.getIsDefault())
                    .priority(request.getPriority())
                    .build();

            CategoryWarehouse saved = categoryWarehouseRepository.save(assignment);
            results.add(mapper.toDto(saved, warehouse.getName()));
            newlyAssigned.add(category);
        }

        logResults(newlyAssigned, alreadyAssigned, request.getWarehouseId(), createdBy);

        return new BulkAssignmentResult(results, newlyAssigned, alreadyAssigned);
    }

    /**
     * Logs the results of bulk assignment.
     *
     * @param newlyAssigned   list of newly assigned categories
     * @param alreadyAssigned list of already assigned categories
     * @param warehouseId     the warehouse ID
     * @param createdBy       ID of the user performing the operation
     */
    private void logResults(List<String> newlyAssigned, List<String> alreadyAssigned,
                            Long warehouseId, Long createdBy) {
        if (!newlyAssigned.isEmpty()) {
            log.info(logMsg.get("category.bulk.assigned.log",
                    newlyAssigned.size(), warehouseId, createdBy));
        }

        if (!alreadyAssigned.isEmpty()) {
            log.warn(logMsg.get("category.bulk.already.assigned.log",
                    String.join(", ", alreadyAssigned)));
        }
    }

    /**
     * Result of bulk category assignment operation.
     *
     * @param results         successfully assigned categories with details
     * @param newlyAssigned   list of newly assigned category names
     * @param alreadyAssigned list of category names that were already assigned
     */
    public record BulkAssignmentResult(
            List<CategoryWarehouseDto> results,
            List<String> newlyAssigned,
            List<String> alreadyAssigned
    ) {}
}