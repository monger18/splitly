package com.satyam.splitly.expense.split;

import com.satyam.splitly.common.money.MoneyUtils;
import com.satyam.splitly.expense.SplitType;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Split by weight - 2:1:1 when one person ate twice as much.
 *
 * <p>More forgiving than PERCENTAGE because the weights are relative: they can be any
 * non-negative numbers and need not total anything in particular. A weight of zero is
 * allowed, for someone who is present but not paying a share.
 */
@Component
public class SharesSplitStrategy implements SplitStrategy {

    @Override
    public SplitType type() {
        return SplitType.SHARES;
    }

    @Override
    public List<SplitShare> split(BigDecimal total, List<SplitInput> inputs) {
        SplitShares.requireAllValuesPresent(inputs, "a share weight");
        SplitShares.requireNonNegative(inputs, "Share weights");

        List<BigDecimal> weights = inputs.stream().map(SplitInput::value).toList();
        return SplitShares.zip(inputs, MoneyUtils.allocate(total, weights));
    }
}
