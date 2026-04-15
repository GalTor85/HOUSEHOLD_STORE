package ru.galtor85.household_store.processor.price;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.cart.CartItemDto;
import ru.galtor85.household_store.dto.response.finance.PriceCalculationResult;
import ru.galtor85.household_store.entity.promotion.PriceRule;
import ru.galtor85.household_store.entity.user.UserType;
import ru.galtor85.household_store.repository.price.PriceRuleRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Processor for applying price rules to orders.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriceRuleProcessor {

    private static final String DISCOUNT_TYPE_RULE = "RULE";
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final String DIGITS_ONLY_PATTERN = "\\d+";
    private static final String RULE_SEPARATOR = ":";
    private static final String PRODUCT_ID_SEPARATOR = ",";
    private static final String CATEGORY_INFO_FORMAT = "%s: %d";
    private static final String CATEGORY_INFO_DELIMITER = ", ";
    private static final String UNKNOWN_CATEGORY = "unknown";
    private static final int RULE_PARTS_LENGTH_3 = 3;
    private static final int RULE_PARTS_LENGTH_2 = 2;

    private final PriceRuleRepository priceRuleRepository;
    private final LogMessageService logMsg;

    /**
     * Applies active price rules to the current total.
     *
     * @param currentTotal current order total
     * @param userType user type for rule filtering
     * @param items cart items
     * @param appliedDiscounts list to track applied discounts
     * @return total after applying rules
     */
    public BigDecimal applyPriceRules(BigDecimal currentTotal, UserType userType,
                                      List<CartItemDto> items,
                                      List<PriceCalculationResult.AppliedDiscount> appliedDiscounts) {
        List<PriceRule> activeRules = priceRuleRepository.findActiveRulesForUserType(
                userType, LocalDateTime.now());

        if (activeRules.isEmpty()) {
            log.debug(logMsg.get("price.rules.none.active"));
            return currentTotal;
        }

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
                        .type(DISCOUNT_TYPE_RULE)
                        .build());
                log.debug(logMsg.get("price.rule.applied", rule.getName(), discount));
            }
        }
        return result;
    }

    private BigDecimal applySingleRule(BigDecimal currentTotal, PriceRule rule,
                                       List<CartItemDto> items) {
        return switch (rule.getDiscountType()) {
            case PERCENTAGE -> applyPercentageDiscount(currentTotal, rule);
            case FIXED_AMOUNT -> applyFixedDiscount(currentTotal, rule);
            case BUY_X_GET_Y -> applyBuyXGetY(currentTotal, rule, items);
            case FREE_SHIPPING -> currentTotal;
            case BUNDLE -> applyBundleDiscount(currentTotal, rule, items);
        };
    }

    private BigDecimal applyPercentageDiscount(BigDecimal currentTotal, PriceRule rule) {
        BigDecimal result = currentTotal.multiply(BigDecimal.ONE.subtract(
                rule.getDiscountValue().divide(ONE_HUNDRED, RoundingMode.HALF_UP)));
        log.debug(logMsg.get("price.rule.percentage.applied",
                rule.getName(), rule.getDiscountValue(), result));
        return result;
    }

    private BigDecimal applyFixedDiscount(BigDecimal currentTotal, PriceRule rule) {
        BigDecimal result = currentTotal.subtract(rule.getDiscountValue()).max(BigDecimal.ZERO);
        log.debug(logMsg.get("price.rule.fixed.applied",
                rule.getName(), rule.getDiscountValue(), result));
        return result;
    }

    private BigDecimal applyBuyXGetY(BigDecimal currentTotal, PriceRule rule,
                                     List<CartItemDto> items) {
        String ruleValue = rule.getDiscountValue().toString();
        try {
            String[] parts = ruleValue.split(RULE_SEPARATOR);
            if (parts.length == RULE_PARTS_LENGTH_3) {
                String target = parts[0];
                int buyQuantity = Integer.parseInt(parts[1]);
                int freeQuantity = Integer.parseInt(parts[2]);

                log.debug(logMsg.get("price.rule.buyxgety.processing",
                        rule.getName(), target, buyQuantity, freeQuantity));

                List<CartItemDto> eligibleItems = findEligibleItems(items, target);

                if (eligibleItems.isEmpty()) {
                    log.debug(logMsg.get("price.rule.buyxgety.no.items", rule.getName(), target));
                    return currentTotal;
                }

                log.debug(logMsg.get("price.rule.buyxgety.items.found", eligibleItems.size(), target));

                BigDecimal discount = calculateBuyXGetYDiscount(
                        eligibleItems, buyQuantity, freeQuantity, rule.getName());
                log.debug(logMsg.get("price.rule.buyxgety.applied", rule.getName(), discount));

                return currentTotal.subtract(discount);
            } else {
                log.warn(logMsg.get("price.rule.buyxgety.invalid.format", ruleValue));
            }
        } catch (Exception e) {
            log.error(logMsg.get("price.rule.buyxgety.error", rule.getName(), e.getMessage()), e);
        }
        return currentTotal;
    }

    private List<CartItemDto> findEligibleItems(List<CartItemDto> items, String target) {
        if (target.matches(DIGITS_ONLY_PATTERN)) {
            Long productId = Long.parseLong(target);
            return items.stream()
                    .filter(item -> item.getProductId().equals(productId))
                    .toList();
        }
        return items.stream()
                .filter(item -> target.equals(item.getCategory()))
                .toList();
    }

    private BigDecimal calculateBuyXGetYDiscount(List<CartItemDto> eligibleItems,
                                                 int buyQuantity, int freeQuantity,
                                                 String ruleName) {
        logCategoryInfo(eligibleItems);

        List<CartItemDto> sorted = eligibleItems.stream()
                .sorted(Comparator.comparing(CartItemDto::getPrice))
                .toList();

        int totalEligibleQuantity = eligibleItems.stream().mapToInt(CartItemDto::getQuantity).sum();
        int freeUnits = (totalEligibleQuantity / (buyQuantity + freeQuantity)) * freeQuantity;

        log.debug(logMsg.get("price.rule.buyxgety.calculation",
                totalEligibleQuantity, buyQuantity, freeQuantity, freeUnits));

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

        logDiscountDetails(discountDetails);
        log.info(logMsg.get("price.rule.buyxgety.total.discount", ruleName, discount, freeUnits));

        return discount;
    }

    private void logCategoryInfo(List<CartItemDto> eligibleItems) {
        Map<String, Long> categoryCount = eligibleItems.stream()
                .collect(Collectors.groupingBy(CartItemDto::getCategory, Collectors.counting()));

        if (!categoryCount.isEmpty()) {
            String categoriesInfo = categoryCount.entrySet().stream()
                    .map(e -> String.format(CATEGORY_INFO_FORMAT, e.getKey(), e.getValue()))
                    .collect(Collectors.joining(CATEGORY_INFO_DELIMITER));
            log.debug(logMsg.get("price.rule.buyxgety.eligible.categories", categoriesInfo));
        } else {
            log.debug(logMsg.get("price.rule.buyxgety.no.categories"));
        }
    }

    private void logDiscountDetails(List<DiscountDetail> discountDetails) {
        for (DiscountDetail detail : discountDetails) {
            log.debug(logMsg.get("price.rule.buyxgety.discount.detail",
                    detail.productName(),
                    detail.quantity(),
                    detail.discount(),
                    Objects.requireNonNullElse(detail.category(), UNKNOWN_CATEGORY)));
        }
    }

    private record DiscountDetail(Long productId, String productName, String category, int quantity,
                                  BigDecimal discount) {}

    private BigDecimal applyBundleDiscount(BigDecimal currentTotal, PriceRule rule,
                                           List<CartItemDto> items) {
        String ruleValue = rule.getDiscountValue().toString();
        try {
            String[] parts = ruleValue.split(RULE_SEPARATOR);
            if (parts.length == RULE_PARTS_LENGTH_2) {
                String[] productIds = parts[0].split(PRODUCT_ID_SEPARATOR);
                double discountPercent = Double.parseDouble(parts[1]);

                if (hasAllBundleItems(items, productIds)) {
                    BigDecimal bundleTotal = calculateBundleTotal(items, productIds);
                    BigDecimal bundleDiscount = bundleTotal
                            .multiply(BigDecimal.valueOf(discountPercent))
                            .divide(ONE_HUNDRED, RoundingMode.HALF_UP);

                    log.debug(logMsg.get("price.rule.bundle.applied", rule.getName(), bundleDiscount));
                    return currentTotal.subtract(bundleDiscount);
                }
            }
        } catch (Exception e) {
            log.error(logMsg.get("price.rule.bundle.error", rule.getName(), e.getMessage()), e);
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