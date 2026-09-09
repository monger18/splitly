package com.satyam.splitly.expense.split;

import com.satyam.splitly.expense.SplitType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Finds the strategy for a split type.
 *
 * <p>Spring injects every {@link SplitStrategy} bean it can find into the constructor -
 * asking for a {@code List<SomeInterface>} gets all implementations of it. Each one is
 * asked which type it handles and the map is built from the answers, so this class never
 * names a concrete strategy and never needs editing when one is added.
 *
 * <p>The startup check matters as much as the lookup: if a SplitType has no strategy, the
 * application refuses to start rather than failing on the first request that happens to
 * use it. Wiring mistakes should surface at boot, not in production.
 */
@Component
public class SplitStrategyResolver {

    private final Map<SplitType, SplitStrategy> strategies = new EnumMap<>(SplitType.class);

    public SplitStrategyResolver(List<SplitStrategy> discovered) {
        for (SplitStrategy strategy : discovered) {
            SplitStrategy existing = strategies.put(strategy.type(), strategy);
            if (existing != null) {
                throw new IllegalStateException("Two strategies claim " + strategy.type()
                        + ": " + existing.getClass().getSimpleName() + " and " + strategy.getClass().getSimpleName());
            }
        }
        for (SplitType type : SplitType.values()) {
            if (!strategies.containsKey(type)) {
                throw new IllegalStateException("No SplitStrategy handles " + type);
            }
        }
    }

    public SplitStrategy resolve(SplitType type) {
        return strategies.get(type);
    }
}
