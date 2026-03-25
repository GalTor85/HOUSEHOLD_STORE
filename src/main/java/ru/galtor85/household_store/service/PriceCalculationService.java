package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.galtor85.household_store.dto.CartItemDto;
import ru.galtor85.household_store.dto.PriceCalculationRequest;
import ru.galtor85.household_store.dto.PriceCalculationResult;
import ru.galtor85.household_store.processor.PriceCalculationProcessor;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceCalculationService {

    private final PriceCalculationProcessor priceCalculationProcessor;
    private final MessageService messageService;

    /**
     * Рассчитывает итоговую цену
     */
    public PriceCalculationResult calculatePrice(PriceCalculationRequest request) {
        log.debug(messageService.get("price.calculation.service.start"));
        return priceCalculationProcessor.calculatePrice(request);
    }

    /**
     * Рассчитывает цену для списка товаров
     */
    public PriceCalculationResult calculatePriceForItems(List<CartItemDto> items, Long userId) {
        log.debug(messageService.get("price.calculation.service.for.items.start", userId));
        return priceCalculationProcessor.calculatePriceForItems(items, userId);
    }

    /**
     * Рассчитывает цену с промокодом
     */
    public PriceCalculationResult calculatePriceWithPromoCode(List<CartItemDto> items,
                                                              Long userId,
                                                              String promoCode) {
        log.debug(messageService.get("price.calculation.service.with.promo.start", promoCode));
        return priceCalculationProcessor.calculatePriceWithPromoCode(items, userId, promoCode);
    }

    /**
     * Рассчитывает только базовую цену
     */
    public BigDecimal calculateBasePrice(List<CartItemDto> items) {
        log.debug(messageService.get("price.calculation.service.base.start"));
        return priceCalculationProcessor.calculateBasePrice(items);
    }
}