package com.satyam.splitly.expense.split;

import com.satyam.splitly.expense.SplitType;
import java.math.BigDecimal;
import java.util.List;

/**
 * One way of dividing an expense among its participants.
 *
 * <p>Each implementation is a Spring bean that declares which {@link SplitType} it
 * handles; {@link SplitStrategyResolver} builds the lookup from that. Adding a fifth way
 * to split - by number of nights stayed, say - means adding one enum constant and one
 * class. No existing file is edited, and there is no {@code switch} anywhere to forget
 * to update.
 *
 * <p><strong>Every implementation must guarantee that the returned amounts sum to
 * exactly {@code total}.</strong> Three of the four get this for free by delegating to
 * {@link com.satyam.splitly.common.money.MoneyUtils#allocate}; EXACT gets it by refusing
 * input that does not already add up.
 */
public interface SplitStrategy {

    SplitType type();

    /**
     * @throws com.satyam.splitly.common.exception.BadRequestException if the inputs do
     *         not satisfy this strategy's rules
     */
    List<SplitShare> split(BigDecimal total, List<SplitInput> inputs);
}
