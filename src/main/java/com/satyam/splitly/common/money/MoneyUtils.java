package com.satyam.splitly.common.money;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Splitting money without losing or inventing any.
 *
 * <p>The problem this solves: 10.00 divided three ways is 3.333..., and no rounding of
 * that on its own adds back up to 10.00. Round each share to 3.33 and 0.01 vanishes;
 * round each to 3.34 and 0.02 appears. In an expense splitter either is a bug, because
 * every balance in the group is derived from these numbers and the errors accumulate.
 *
 * <p>The fix is to stop rounding independently. {@link #allocate} works in whole minor
 * units - paise, cents - so nothing fractional ever survives, and it hands out the
 * leftover units explicitly. The result is guaranteed: <strong>the shares always sum to
 * exactly the total.</strong>
 *
 * <p>The distribution rule is the largest remainder method, also called Hamilton's
 * method: give every participant their floored share, then hand the leftover units, one
 * each, to whoever was rounded down the hardest. Ties go to the earlier participant, so
 * the result is deterministic and the same input always produces the same output - which
 * is what makes it testable.
 */
public final class MoneyUtils {

    /** Money is stored as numeric(19,2), so two decimal places everywhere. */
    public static final int SCALE = 2;

    private MoneyUtils() {
    }

    /** Rounds to the storage scale. The only place rounding is allowed to happen. */
    public static BigDecimal normalise(BigDecimal amount) {
        return amount.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.signum() > 0;
    }

    /**
     * Splits {@code total} into {@code parts} shares that differ by at most one minor
     * unit and sum exactly to the total.
     */
    public static List<BigDecimal> allocateEqually(BigDecimal total, int parts) {
        if (parts <= 0) {
            throw new IllegalArgumentException("Need at least one participant");
        }
        return allocate(total, java.util.Collections.nCopies(parts, BigDecimal.ONE));
    }

    /**
     * Splits {@code total} in proportion to {@code weights}.
     *
     * <p>One method covers three of the four split types, because the weights are all
     * that differ: every weight 1 for an equal split, the percentages for a percentage
     * split, the share counts for a 2:1:1 split. Only EXACT bypasses it, since the caller
     * has already supplied the amounts.
     *
     * @return one amount per weight, in the same order, summing exactly to {@code total}
     */
    public static List<BigDecimal> allocate(BigDecimal total, List<BigDecimal> weights) {
        if (weights == null || weights.isEmpty()) {
            throw new IllegalArgumentException("Need at least one participant");
        }
        if (weights.stream().anyMatch(weight -> weight.signum() < 0)) {
            throw new IllegalArgumentException("Weights cannot be negative");
        }

        BigDecimal totalWeight = weights.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalWeight.signum() <= 0) {
            throw new IllegalArgumentException("Weights must add up to more than zero");
        }

        // Whole paise from here on. Nothing fractional can survive an integer.
        long totalMinor = toMinorUnits(total);
        int size = weights.size();

        long[] shares = new long[size];
        BigDecimal[] remainders = new BigDecimal[size];
        long allocated = 0;

        for (int i = 0; i < size; i++) {
            BigDecimal exact = BigDecimal.valueOf(totalMinor)
                    .multiply(weights.get(i))
                    .divide(totalWeight, MathContext.DECIMAL128);

            BigDecimal floor = exact.setScale(0, RoundingMode.FLOOR);
            shares[i] = floor.longValueExact();
            remainders[i] = exact.subtract(floor);
            allocated += shares[i];
        }

        // Flooring loses less than one unit each, so this is always between 0 and size-1.
        long leftover = totalMinor - allocated;

        List<Integer> order = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            order.add(i);
        }
        order.sort(Comparator
                .comparing((Integer i) -> remainders[i]).reversed()   // rounded down hardest first
                .thenComparing(Comparator.naturalOrder()));           // ties: earlier participant

        for (int k = 0; k < leftover; k++) {
            shares[order.get(k)]++;
        }

        List<BigDecimal> result = new ArrayList<>(size);
        for (long share : shares) {
            result.add(fromMinorUnits(share));
        }
        return result;
    }

    public static BigDecimal sum(List<BigDecimal> amounts) {
        return amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Compares by value, not by {@code equals}. {@code BigDecimal.equals} also compares
     * scale, so {@code 100.0} and {@code 100.00} are not equal to it - a trap that shows
     * up constantly when comparing a database value with a computed one.
     */
    public static boolean equalAmounts(BigDecimal left, BigDecimal right) {
        return left != null && right != null && left.compareTo(right) == 0;
    }

    private static long toMinorUnits(BigDecimal amount) {
        return normalise(amount).movePointRight(SCALE).longValueExact();
    }

    private static BigDecimal fromMinorUnits(long minorUnits) {
        return BigDecimal.valueOf(minorUnits, SCALE);
    }
}
