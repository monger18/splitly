package com.satyam.splitly.common.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MoneyUtilsTest {

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    @Nested
    @DisplayName("allocateEqually")
    class AllocateEqually {

        @Test
        @DisplayName("divides cleanly when it can")
        void divides_cleanly() {
            assertThat(MoneyUtils.allocateEqually(money("900.00"), 3))
                    .containsExactly(money("300.00"), money("300.00"), money("300.00"));
        }

        @Test
        @DisplayName("gives the leftover paisa to the first participant, losing nothing")
        void handles_the_indivisible_case() {
            List<BigDecimal> shares = MoneyUtils.allocateEqually(money("10.00"), 3);

            assertThat(shares).containsExactly(money("3.34"), money("3.33"), money("3.33"));
            assertThat(MoneyUtils.sum(shares)).isEqualByComparingTo(money("10.00"));
        }

        @Test
        @DisplayName("a single participant owes the whole amount")
        void single_participant() {
            assertThat(MoneyUtils.allocateEqually(money("42.42"), 1)).containsExactly(money("42.42"));
        }

        @Test
        void rejects_zero_participants() {
            assertThatThrownBy(() -> MoneyUtils.allocateEqually(money("10.00"), 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("allocate by weight")
    class AllocateByWeight {

        @Test
        @DisplayName("2:1:1 of 100.00")
        void weighted() {
            assertThat(MoneyUtils.allocate(money("100.00"), List.of(money("2"), money("1"), money("1"))))
                    .containsExactly(money("50.00"), money("25.00"), money("25.00"));
        }

        @Test
        @DisplayName("the leftover goes to whoever was rounded down hardest, not to the first")
        void largest_remainder_wins() {
            // 33.33 / 33.33 / 33.34 of 10.00 floors to 3.33 each, leaving one paisa.
            // The third participant has the biggest fractional part, so it goes there.
            List<BigDecimal> shares = MoneyUtils.allocate(
                    money("10.00"), List.of(money("33.33"), money("33.33"), money("33.34")));

            assertThat(shares).containsExactly(money("3.33"), money("3.33"), money("3.34"));
            assertThat(MoneyUtils.sum(shares)).isEqualByComparingTo(money("10.00"));
        }

        @Test
        @DisplayName("a zero weight owes nothing")
        void zero_weight() {
            assertThat(MoneyUtils.allocate(money("60.00"), List.of(money("1"), money("0"), money("1"))))
                    .containsExactly(money("30.00"), money("0.00"), money("30.00"));
        }

        @Test
        void rejects_negative_weights() {
            assertThatThrownBy(() -> MoneyUtils.allocate(money("10.00"), List.of(money("-1"), money("2"))))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_weights_that_are_all_zero() {
            assertThatThrownBy(() -> MoneyUtils.allocate(money("10.00"), List.of(money("0"), money("0"))))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @ParameterizedTest(name = "{0} split {1} ways")
    @CsvSource({
            "0.01, 3", "0.02, 3", "10.00, 3", "100.00, 7", "0.05, 4",
            "999999.99, 11", "1.00, 100", "7.77, 6",
    })
    @DisplayName("shares always sum back to the total")
    void equal_split_never_loses_money(String total, int parts) {
        List<BigDecimal> shares = MoneyUtils.allocateEqually(money(total), parts);

        assertThat(shares).hasSize(parts);
        assertThat(MoneyUtils.sum(shares)).isEqualByComparingTo(money(total));
    }

    @Test
    @DisplayName("property: 2000 random splits all reconcile exactly, and stay within one paisa of each other")
    void randomised_invariant() {
        Random random = new Random(20260906L);   // fixed seed: a failure is reproducible

        for (int run = 0; run < 2000; run++) {
            BigDecimal total = BigDecimal.valueOf(random.nextInt(10_000_000) + 1, 2);
            int parts = random.nextInt(12) + 1;

            List<BigDecimal> shares = MoneyUtils.allocateEqually(total, parts);

            assertThat(MoneyUtils.sum(shares))
                    .as("total %s split %d ways", total, parts)
                    .isEqualByComparingTo(total);

            BigDecimal max = shares.stream().max(BigDecimal::compareTo).orElseThrow();
            BigDecimal min = shares.stream().min(BigDecimal::compareTo).orElseThrow();
            assertThat(max.subtract(min))
                    .as("fairness for %s split %d ways", total, parts)
                    .isLessThanOrEqualTo(money("0.01"));
        }
    }
}
