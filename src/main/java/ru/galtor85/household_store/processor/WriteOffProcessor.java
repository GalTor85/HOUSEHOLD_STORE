package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.dto.StockWriteOffItem;
import ru.galtor85.household_store.dto.StockWriteOffRequest;
import ru.galtor85.household_store.entity.Product;
import ru.galtor85.household_store.repository.ProductRepository;
import ru.galtor85.household_store.service.MessageService;
import ru.galtor85.household_store.util.EntityFinder;
import ru.galtor85.household_store.validator.ValidationHelper;

@Slf4j
@Component
@RequiredArgsConstructor
public class WriteOffProcessor {

    private final EntityFinder entityFinder;
    private final ValidationHelper validationHelper;
    private final ProductRepository productRepository;
    private final MessageService messageService;

    @Transactional
    public void processWriteOff(StockWriteOffRequest request, Long managerId) {
        for (StockWriteOffItem item : request.getItems()) {
            Product product = entityFinder.findProductById(item.getProductId());
            validationHelper.validateStockAvailability(product, item.getQuantity());

            product.setQuantityInStock(product.getQuantityInStock() - item.getQuantity());
            productRepository.save(product);

            log.info(messageService.get("manager.writeoff.executed.log",
                    item.getProductId(), item.getQuantity(), request.getReason(), managerId));
        }
    }
}