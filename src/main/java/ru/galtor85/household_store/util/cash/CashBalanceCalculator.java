package ru.galtor85.household_store.util.cash;

import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.finance.CashTransaction;
import ru.galtor85.household_store.entity.finance.Invoice;
import ru.galtor85.household_store.entity.finance.TransactionType;
import ru.galtor85.household_store.entity.order.OrderType;

import java.math.BigDecimal;

@Component
public class CashBalanceCalculator {

    /**
     * Calculates balance after transaction from existing CashTransaction.
     *
     * @param transaction the cash transaction
     * @param invoice the invoice (can be null, will use transaction.getInvoice())
     * @param balanceBefore balance before transaction
     * @return balance after transaction
     */
    public BigDecimal getBalanceAfter(CashTransaction transaction, Invoice invoice, BigDecimal balanceBefore) {
        if (transaction.getTransactionType() == null || transaction.getAmount() == null) {
            return balanceBefore;
        }

        OrderType orderType = getOrderType(transaction, invoice);
        int multiplier = transaction.getTransactionType().getMultiplier(orderType);

        return balanceBefore.add(
                transaction.getAmount().multiply(BigDecimal.valueOf(multiplier))
        );
    }

    /**
     * Calculates balance after transaction from parameters.
     *
     * @param currentBalance balance before transaction
     * @param transactionType type of transaction
     * @param amount transaction amount
     * @param invoice invoice for order type determination
     * @return balance after transaction
     */
    public BigDecimal getBalanceAfter(BigDecimal currentBalance,
                                      TransactionType transactionType,
                                      BigDecimal amount,
                                      Invoice invoice) {
        if (transactionType == null || amount == null) {
            return currentBalance;
        }

        OrderType orderType = getOrderType(invoice);
        int multiplier = transactionType.getMultiplier(orderType);

        return currentBalance.add(amount.multiply(BigDecimal.valueOf(multiplier)));
    }

    private OrderType getOrderType(CashTransaction transaction, Invoice invoice) {
        if (invoice != null) {
            return invoice.isSalesOrder() ? OrderType.SALES : OrderType.PURCHASE;
        }
        if (transaction.getInvoice() != null) {
            return transaction.getInvoice().isSalesOrder() ? OrderType.SALES : OrderType.PURCHASE;
        }
        return null;
    }

    private OrderType getOrderType(Invoice invoice) {
        if (invoice == null) {
            return null;
        }
        return invoice.isSalesOrder() ? OrderType.SALES : OrderType.PURCHASE;
    }
}

