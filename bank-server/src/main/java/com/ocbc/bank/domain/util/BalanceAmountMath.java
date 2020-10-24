package com.ocbc.bank.domain.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class BalanceAmountMath {
    public static BigDecimal getPossibleTransferAmount(BigDecimal expectedAmount, BigDecimal availableAmount) {
        return compareTo(expectedAmount, availableAmount) < 1 ? expectedAmount : availableAmount;
    }

    public static BigDecimal add(BigDecimal currentAmount, BigDecimal remainder) {
        return currentAmount.add(remainder).setScale(2, RoundingMode.HALF_DOWN);
    }

    public static BigDecimal subtract(BigDecimal currentAmount, BigDecimal remainder) {
        return currentAmount.subtract(remainder).setScale(2, RoundingMode.HALF_DOWN);
    }

    public static BigDecimal get(BigDecimal currentAmount) {
        return currentAmount.setScale(2, RoundingMode.HALF_DOWN);
    }

    public static BigDecimal get(String amountString) {
        return new BigDecimal(amountString).setScale(2, RoundingMode.HALF_DOWN);
    }

    public static boolean equals(BigDecimal amount1, BigDecimal amount2) {
        return compareTo(amount1, amount2) == 0;
    }

    public static int compareTo(BigDecimal amount1, BigDecimal amount2) {
        return get(amount1).compareTo(get(amount2));
    }


}
