package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.CartItemDto;
import ru.galtor85.household_store.dto.PriceCalculationResult;
import ru.galtor85.household_store.entity.PriceRule;
import ru.galtor85.household_store.entity.UserType;
import ru.galtor85.household_store.repository.PriceRuleRepository;
import ru.galtor85.household_store.service.MessageService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriceRuleProcessor {

    private final PriceRuleRepository priceRuleRepository;
    private final MessageService messageService;

    public BigDecimal applyPriceRules(BigDecimal currentTotal, UserType userType,
                                      List<CartItemDto> items,
                                      List<PriceCalculationResult.AppliedDiscount> appliedDiscounts) {
        List<PriceRule> activeRules = priceRuleRepository.findActiveRulesForUserType(
                userType, LocalDateTime.now());

        if (activeRules.isEmpty()) {
            log.debug(messageService.get("price.rules.none.active"));
            return currentTotal;
        }

        // Сортируем по приоритету
        activeRules.sort(Comparator.comparing(PriceRule::getPriority));

        BigDecimal result = currentTotal;
        for (PriceRule rule : activeRules) {
            BigDecimal beforeRule = result;
            result = applySingleRule(result, rule, items);

            if (result.compareTo(beforeRule) < 0) {
                BigDecimal discount = beforeRule.subtract(result);
                appliedDiscounts.add(PriceCalculationResult.AppliedDiscount.builder()
                        .name(rule.getName())
                        .description(rule.getDescription())
                        .discountAmount(discount)
                        .type("RULE")
                        .build());

                log.debug(messageService.get("price.rule.applied",
                        rule.getName(), discount));
            }
        }
        return result;
    }

    private BigDecimal applySingleRule(BigDecimal currentTotal, PriceRule rule,
                                       List<CartItemDto> items) {
        return switch (rule.getDiscountType()) {
            case PERCENTAGE -> {
                BigDecimal result = applyPercentageDiscount(currentTotal, rule);
                log.debug(messageService.get("price.rule.percentage.applied",
                        rule.getName(), rule.getDiscountValue(), result));
                yield result;
            }

            case FIXED_AMOUNT -> {
                BigDecimal result = applyFixedDiscount(currentTotal, rule);
                log.debug(messageService.get("price.rule.fixed.applied",
                        rule.getName(), rule.getDiscountValue(), result));
                yield result;
            }

            case BUY_X_GET_Y -> {
                BigDecimal result = applyBuyXGetY(currentTotal, rule, items);
                log.debug(messageService.get("price.rule.buyxgety.completed",
                        rule.getName()));
                yield result;
            }

            case FREE_SHIPPING -> {
                log.debug(messageService.get("price.rule.free.shipping", rule.getName()));
                yield currentTotal; // Бесплатная доставка не влияет на сумму товаров
            }

            case BUNDLE -> {
                BigDecimal result = applyBundleDiscount(currentTotal, rule, items);
                log.debug(messageService.get("price.rule.bundle.completed",
                        rule.getName()));
                yield result;
            }
        };
    }

    private BigDecimal applyPercentageDiscount(BigDecimal currentTotal, PriceRule rule) {
        return currentTotal.multiply(BigDecimal.ONE.subtract(
                rule.getDiscountValue().divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP)));
    }

    private BigDecimal applyFixedDiscount(BigDecimal currentTotal, PriceRule rule) {
        return currentTotal.subtract(rule.getDiscountValue()).max(BigDecimal.ZERO);
    }

    private BigDecimal applyBuyXGetY(BigDecimal currentTotal, PriceRule rule,
                                     List<CartItemDto> items) {
        String ruleValue = rule.getDiscountValue().toString();
        try {
            String[] parts = ruleValue.split(":");
            if (parts.length == 3) {
                String target = parts[0];
                int buyQuantity = Integer.parseInt(parts[1]);
                int freeQuantity = Integer.parseInt(parts[2]);

                log.debug(messageService.get("price.rule.buyxgety.processing",
                        rule.getName(), target, buyQuantity, freeQuantity));

                // Находим подходящие товары
                List<CartItemDto> eligibleItems = findEligibleItems(items, target);

                if (eligibleItems.isEmpty()) {
                    log.debug(messageService.get("price.rule.buyxgety.no.items",
                            rule.getName(), target));
                    return currentTotal;
                }

                log.debug(messageService.get("price.rule.buyxgety.items.found",
                        eligibleItems.size(), target));

                // Рассчитываем скидку
                BigDecimal discount = calculateBuyXGetYDiscount(
                        eligibleItems, buyQuantity, freeQuantity, rule.getName());

                log.debug(messageService.get("price.rule.buyxgety.applied",
                        rule.getName(), discount));

                return currentTotal.subtract(discount);
            } else {
                log.warn(messageService.get("price.rule.buyxgety.invalid.format",
                        ruleValue));
            }
        } catch (Exception e) {
            log.error(messageService.get("price.rule.buyxgety.error",
                    rule.getName(), e.getMessage()), e);
        }
        return currentTotal;
    }

    private List<CartItemDto> findEligibleItems(List<CartItemDto> items, String target) {
        // Проверяем, является ли target числом (ID товара) или строкой (категория)
        if (target.matches("\\d+")) {
            Long productId = Long.parseLong(target);
            return items.stream()
                    .filter(item -> item.getProductId().equals(productId))
                    .collect(Collectors.toList());
        } else {
            // ТЕПЕРЬ МОЖНО ИСКАТЬ ПО КАТЕГОРИИ НАПРЯМУЮ
            return items.stream()
                    .filter(item -> target.equals(item.getCategory()))
                    .collect(Collectors.toList());
        }
    }

    private BigDecimal calculateBuyXGetYDiscount(List<CartItemDto> eligibleItems,
                                                 int buyQuantity, int freeQuantity,
                                                 String ruleName) {

        // Логируем информацию о категориях
        Map<String, Long> categoryCount = eligibleItems.stream()
                .collect(Collectors.groupingBy(
                        CartItemDto::getCategory,
                        Collectors.counting()
                ));

        if (!categoryCount.isEmpty()) {
            String categoriesInfo = categoryCount.entrySet().stream()
                    .map(e -> String.format("%s: %d", e.getKey(), e.getValue()))
                    .collect(Collectors.joining(", "));

            log.debug(messageService.get("price.rule.buyxgety.eligible.categories",
                    categoriesInfo));
        } else {
            log.debug(messageService.get("price.rule.buyxgety.no.categories"));
        }

        // Сортируем по цене (самые дешевые будут бесплатными)
        List<CartItemDto> sorted = eligibleItems.stream()
                .sorted(Comparator.comparing(CartItemDto::getPrice))
                .collect(Collectors.toList());

        // Рассчитываем количество бесплатных единиц
        int totalEligibleQuantity = eligibleItems.stream()
                .mapToInt(CartItemDto::getQuantity)
                .sum();

        int freeUnits = (totalEligibleQuantity / (buyQuantity + freeQuantity)) * freeQuantity;

        log.debug(messageService.get("price.rule.buyxgety.calculation",
                totalEligibleQuantity, buyQuantity, freeQuantity, freeUnits));

        // Берем самые дешевые товары как бесплатные
        BigDecimal discount = BigDecimal.ZERO;
        int remainingFree = freeUnits;
        List<DiscountDetail> discountDetails = new ArrayList<>();

        for (CartItemDto item : sorted) {
            if (remainingFree <= 0) break;

            int freeFromThisItem = Math.min(item.getQuantity(), remainingFree);
            BigDecimal itemDiscount = item.getPrice().multiply(BigDecimal.valueOf(freeFromThisItem));
            discount = discount.add(itemDiscount);

            discountDetails.add(new DiscountDetail(
                    item.getProductId(),
                    item.getProductName(),
                    item.getCategory(),
                    freeFromThisItem,
                    itemDiscount
            ));

            remainingFree -= freeFromThisItem;
        }

        // Логируем детали скидки
        for (DiscountDetail detail : discountDetails) {
            log.debug(messageService.get("price.rule.buyxgety.discount.detail",
                    detail.getProductName(),
                    detail.getQuantity(),
                    detail.getDiscount(),
                    detail.getCategory() != null ? detail.getCategory() : "без категории"));
        }

        log.info(messageService.get("price.rule.buyxgety.total.discount",
                ruleName, discount, freeUnits));

        return discount;
    }

    // Вспомогательный класс для деталей скидки
    @lombok.Value
    private static class DiscountDetail {
        Long productId;
        String productName;
        String category;
        int quantity;
        BigDecimal discount;
    }

    private BigDecimal applyBundleDiscount(BigDecimal currentTotal, PriceRule rule,
                                           List<CartItemDto> items) {
        // Формат: "productId1,productId2,productId3:discountPercent"
        String ruleValue = rule.getDiscountValue().toString();
        try {
            String[] parts = ruleValue.split(":");
            if (parts.length == 2) {
                String[] productIds = parts[0].split(",");
                double discountPercent = Double.parseDouble(parts[1]);

                // Проверяем, есть ли все товары комплекта в корзине
                if (hasAllBundleItems(items, productIds)) {
                    // Рассчитываем стоимость комплекта
                    BigDecimal bundleTotal = calculateBundleTotal(items, productIds);

                    // Рассчитываем скидку на комплект
                    BigDecimal bundleDiscount = bundleTotal
                            .multiply(BigDecimal.valueOf(discountPercent))
                            .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);

                    log.debug(messageService.get("price.rule.bundle.applied",
                            rule.getName(), bundleDiscount));

                    return currentTotal.subtract(bundleDiscount);
                }
            }
        } catch (Exception e) {
            log.error(messageService.get("price.rule.bundle.error", rule.getName(), e.getMessage()), e);
        }
        return currentTotal;
    }

    private boolean hasAllBundleItems(List<CartItemDto> items, String[] productIds) {
        Set<String> requiredProductIds = new HashSet<>(Arrays.asList(productIds));
        Set<String> cartProductIds = items.stream()
                .map(item -> String.valueOf(item.getProductId()))
                .collect(Collectors.toSet());

        return cartProductIds.containsAll(requiredProductIds);
    }

    private BigDecimal calculateBundleTotal(List<CartItemDto> items, String[] productIds) {
        Set<Long> bundleProductIds = Arrays.stream(productIds)
                .map(Long::parseLong)
                .collect(Collectors.toSet());

        return items.stream()
                .filter(item -> bundleProductIds.contains(item.getProductId()))
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}