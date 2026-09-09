package com.satyam.splitly.expense.split;

import com.satyam.splitly.common.exception.BadRequestException;
import com.satyam.splitly.common.money.MoneyUtils;
import com.satyam.splitly.expense.SplitType;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The caller states each participant's amount outright.
 *
 * <p>The only strategy that does not compute anything. Its whole job is refusing input
 * that does not already add up to the total, which is what keeps the guarantee in
 * {@link SplitStrategy} true for every implementation.
 */
@Component
public class ExactSplitStrategy implements SplitStrategy {

    @Override
    public SplitType type() {
        return SplitType.EXACT;
    }

    @Override
    public List<SplitShare> split(BigDecimal total, List<SplitInput> inputs) {
        SplitShares.requireAllValuesPresent(inputs, "an exact amount");

        List<BigDecimal> amounts = inputs.stream()
                .map(input -> MoneyUtils.normalise(input.value()))
                .toList();

        BigDecimal sum = MoneyUtils.sum(amounts);
        if (!MoneyUtils.equalAmounts(sum, MoneyUtils.normalise(total))) {
            throw new BadRequestException(
                    "Exact amounts add up to %s but the expense is %s".formatted(sum, MoneyUtils.normalise(total)));
        }
        return SplitShares.zip(inputs, amounts);
    }
}
