package com.satyam.splitly.expense.split;

import com.satyam.splitly.common.money.MoneyUtils;
import com.satyam.splitly.expense.SplitType;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Even split. The {@code value} on each input is ignored.
 *
 * <p>900 across three people is 300 each. 10.00 across three is 3.34, 3.33, 3.33 - the
 * leftover paisa goes to the first participant, and the three still sum to 10.00.
 */
@Component
public class EqualSplitStrategy implements SplitStrategy {

    @Override
    public SplitType type() {
        return SplitType.EQUAL;
    }

    @Override
    public List<SplitShare> split(BigDecimal total, List<SplitInput> inputs) {
        List<BigDecimal> amounts = MoneyUtils.allocateEqually(total, inputs.size());
        return SplitShares.zip(inputs, amounts);
    }
}
