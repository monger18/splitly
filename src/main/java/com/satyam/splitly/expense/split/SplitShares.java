package com.satyam.splitly.expense.split;

import com.satyam.splitly.common.exception.BadRequestException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

/** Validation and assembly shared by the strategies. Package-private on purpose. */
final class SplitShares {

    private SplitShares() {
    }

    static List<SplitShare> zip(List<SplitInput> inputs, List<BigDecimal> amounts) {
        return IntStream.range(0, inputs.size())
                .mapToObj(i -> new SplitShare(inputs.get(i).userId(), amounts.get(i)))
                .toList();
    }

    static void requireAllValuesPresent(List<SplitInput> inputs, String what) {
        boolean anyMissing = inputs.stream().anyMatch(input -> input.value() == null);
        if (anyMissing) {
            throw new BadRequestException("Every participant needs " + what + " for this split type");
        }
    }

    static void requireNonNegative(List<SplitInput> inputs, String what) {
        boolean anyNegative = inputs.stream().anyMatch(input -> input.value().signum() < 0);
        if (anyNegative) {
            throw new BadRequestException(what + " cannot be negative");
        }
    }
}
