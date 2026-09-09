package com.satyam.splitly.expense.split;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.satyam.splitly.common.exception.BadRequestException;
import com.satyam.splitly.common.money.MoneyUtils;
import com.satyam.splitly.expense.SplitType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Plain unit tests - no Spring context, no database. The strategies are ordinary objects
 * with no dependencies, so they can be constructed with {@code new} and run in
 * milliseconds. That is a design property worth keeping.
 */
class SplitStrategyTest {

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    private static SplitInput input(long userId, String value) {
        return new SplitInput(userId, value == null ? null : money(value));
    }

    private static void assertSumsTo(List<SplitShare> shares, String expected) {
        assertThat(MoneyUtils.sum(shares.stream().map(SplitShare::amount).toList()))
                .isEqualByComparingTo(money(expected));
    }

    @Nested
    class Equal {
        private final EqualSplitStrategy strategy = new EqualSplitStrategy();

        @Test
        void splits_evenly_and_ignores_the_supplied_values() {
            List<SplitShare> shares = strategy.split(money("900.00"),
                    List.of(input(1, null), input(2, "999"), input(3, null)));

            assertThat(shares).extracting(SplitShare::amount)
                    .containsExactly(money("300.00"), money("300.00"), money("300.00"));
            assertThat(shares).extracting(SplitShare::userId).containsExactly(1L, 2L, 3L);
        }

        @Test
        void loses_nothing_when_it_does_not_divide() {
            assertSumsTo(strategy.split(money("10.00"),
                    List.of(input(1, null), input(2, null), input(3, null))), "10.00");
        }
    }

    @Nested
    class Exact {
        private final ExactSplitStrategy strategy = new ExactSplitStrategy();

        @Test
        void accepts_amounts_that_add_up() {
            List<SplitShare> shares = strategy.split(money("100.00"),
                    List.of(input(1, "60.00"), input(2, "40.00")));

            assertThat(shares).extracting(SplitShare::amount)
                    .containsExactly(money("60.00"), money("40.00"));
        }

        @Test
        void rejects_amounts_that_do_not_add_up() {
            assertThatThrownBy(() -> strategy.split(money("100.00"),
                    List.of(input(1, "60.00"), input(2, "30.00"))))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("90.00")
                    .hasMessageContaining("100.00");
        }

        @Test
        void rejects_a_missing_amount() {
            assertThatThrownBy(() -> strategy.split(money("100.00"),
                    List.of(input(1, "100.00"), input(2, null))))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    class Percentage {
        private final PercentageSplitStrategy strategy = new PercentageSplitStrategy();

        @Test
        void splits_by_percentage() {
            assertThat(strategy.split(money("200.00"), List.of(input(1, "25"), input(2, "75"))))
                    .extracting(SplitShare::amount)
                    .containsExactly(money("50.00"), money("150.00"));
        }

        @Test
        @DisplayName("awkward percentages still reconcile to the total")
        void awkward_percentages() {
            assertSumsTo(strategy.split(money("10.00"),
                    List.of(input(1, "33.33"), input(2, "33.33"), input(3, "33.34"))), "10.00");
        }

        @Test
        void rejects_percentages_that_are_not_100() {
            assertThatThrownBy(() -> strategy.split(money("100.00"),
                    List.of(input(1, "50"), input(2, "40"))))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("90");
        }
    }

    @Nested
    class Shares {
        private final SharesSplitStrategy strategy = new SharesSplitStrategy();

        @Test
        void splits_by_weight() {
            assertThat(strategy.split(money("100.00"),
                    List.of(input(1, "2"), input(2, "1"), input(3, "1"))))
                    .extracting(SplitShare::amount)
                    .containsExactly(money("50.00"), money("25.00"), money("25.00"));
        }

        @Test
        @DisplayName("weights are relative, so they need not total anything in particular")
        void weights_are_relative() {
            assertSumsTo(strategy.split(money("90.00"),
                    List.of(input(1, "7"), input(2, "3"))), "90.00");
        }

        @Test
        void rejects_negative_weights() {
            assertThatThrownBy(() -> strategy.split(money("100.00"),
                    List.of(input(1, "-1"), input(2, "3"))))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    class Resolver {

        private final List<SplitStrategy> all = List.of(
                new EqualSplitStrategy(), new ExactSplitStrategy(),
                new PercentageSplitStrategy(), new SharesSplitStrategy());

        @Test
        void maps_every_split_type_to_its_strategy() {
            SplitStrategyResolver resolver = new SplitStrategyResolver(all);

            for (SplitType type : SplitType.values()) {
                assertThat(resolver.resolve(type)).isNotNull();
                assertThat(resolver.resolve(type).type()).isEqualTo(type);
            }
        }

        @Test
        @DisplayName("refuses to start if a split type has no strategy")
        void fails_fast_on_a_missing_strategy() {
            assertThatThrownBy(() -> new SplitStrategyResolver(List.of(new EqualSplitStrategy())))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No SplitStrategy handles");
        }
    }
}
