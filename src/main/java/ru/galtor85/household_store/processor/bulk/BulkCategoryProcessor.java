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
import ru.galtor85.household_store.service.i18n.MessageService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BulkCategoryProcessor {

    private final CategoryWarehouseRepository categoryWarehouseRepository;
    private final CategoryWarehouseMapper mapper;
    private final MessageService messageService;

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

    private void logResults(List<String> newlyAssigned, List<String> alreadyAssigned,
                            Long warehouseId, Long createdBy) {
        if (!newlyAssigned.isEmpty()) {
            log.info(messageService.get("category.bulk.assigned.log",
                    newlyAssigned.size(), warehouseId, createdBy));
        }

        if (!alreadyAssigned.isEmpty()) {
            log.warn(messageService.get("category.bulk.already.assigned.log",
                    String.join(", ", alreadyAssigned)));
        }
    }

    @lombok.Value
    public static class BulkAssignmentResult {
        List<CategoryWarehouseDto> results;
        List<String> newlyAssigned;
        List<String> alreadyAssigned;
    }
}