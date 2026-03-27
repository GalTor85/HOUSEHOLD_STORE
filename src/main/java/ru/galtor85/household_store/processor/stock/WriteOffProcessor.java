package ru.galtor85.household_store.processor.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.dto.common.StockWriteOffItem;
import ru.galtor85.household_store.dto.request.stock.StockWriteOffRequest;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.util.entity.EntityFinder;
import ru.galtor85.household_store.validator.common.ValidationHelper;

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