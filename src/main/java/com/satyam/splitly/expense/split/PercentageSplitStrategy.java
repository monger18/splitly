package com.satyam.splitly.expense.split;

import com.satyam.splitly.common.exception.BadRequestException;
import com.satyam.splitly.common.money.MoneyUtils;
import com.satyam.splitly.expense.SplitType;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Split by percentage. The percentages must total 100.
 *
 * <p>The percentages become weights, so the rounding is handled by the same allocation
 * used everywhere else. 33.33% of 10.00 is not a whole number of paise, and this is
 * exactly the case where rounding each share on its own would drop a paisa.
 */
@Component
public class PercentageSplitStrategy implements SplitStrategy {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    @Override
    public SplitType type() {
        return SplitType.PERCENTAGE;
    }

    @Override
    public List<SplitShare> split(BigDecimal total, List<SplitInput> inputs) {
        SplitShares.requireAllValuesPresent(inputs, "a percentage");
        SplitShares.requireNonNegative(inputs, "Percentages");

        List<BigDecimal> percentages = inputs.stream().map(SplitInput::value).toList();
        BigDecimal sum = percentages.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sum.compareTo(ONE_HUNDRED) != 0) {
            throw new BadRequestException("Percentages must add up to 100 but add up to " + sum.stripTrailingZeros().toPlainString());
        }
        return SplitShares.zip(inputs, MoneyUtils.allocate(total, percentages));
    }
}
