package com.satyam.splitly.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DebtSimplifierTest {

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    private static Map<Long, BigDecimal> balances(Object... pairs) {
        Map<Long, BigDecimal> net = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            net.put(((Number) pairs[i]).longValue(), money((String) pairs[i + 1]));
        }
        return net;
    }

    @Test
    @DisplayName("nothing to do when everyone is square")
    void all_settled() {
        assertThat(DebtSimplifier.simplify(balances(1, "0.00", 2, "0.00"))).isEmpty();
        assertThat(DebtSimplifier.simplify(Map.of())).isEmpty();
    }

    @Test
    @DisplayName("one creditor, two debtors")
    void one_creditor_two_debtors() {
        List<Transfer> transfers = DebtSimplifier.simplify(
                balances(1, "600.00", 2, "-300.00", 3, "-300.00"));

        assertThat(transfers).containsExactly(
                new Transfer(2L, 1L, money("300.00")),
                new Transfer(3L, 1L, money("300.00")));
    }

    @Test
    @DisplayName("a chain of debts collapses to one payment")
    void chain_collapses() {
        // Alice paid for Bob, Bob paid for Carol, so Bob nets out to zero entirely.
        // Naively settling expense by expense would be two payments; this is one.
        List<Transfer> transfers = DebtSimplifier.simplify(
                balances(1, "50.00", 2, "0.00", 3, "-50.00"));

        assertThat(transfers).containsExactly(new Transfer(3L, 1L, money("50.00")));
    }

    @Test
    @DisplayName("a debtor is split across two creditors when nobody matches exactly")
    void splits_across_creditors() {
        List<Transfer> transfers = DebtSimplifier.simplify(
                balances(1, "70.00", 2, "30.00", 3, "-100.00"));

        assertThat(transfers).containsExactly(
                new Transfer(3L, 1L, money("70.00")),
                new Transfer(3L, 2L, money("30.00")));
    }

    @Test
    @DisplayName("members already at zero take no part")
    void zero_balances_are_skipped() {
        List<Transfer> transfers = DebtSimplifier.simplify(
                balances(1, "25.00", 2, "0.00", 3, "0.00", 4, "-25.00"));

        assertThat(transfers).containsExactly(new Transfer(4L, 1L, money("25.00")));
    }

    @Test
    @DisplayName("refuses balances that do not sum to zero - money cannot appear from nowhere")
    void rejects_unbalanced_input() {
        assertThatThrownBy(() -> DebtSimplifier.simplify(balances(1, "10.00", 2, "-5.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("do not sum to zero");
    }

    @Test
    @DisplayName("property: settles exactly, and never needs more than n-1 payments")
    void randomised_properties() {
        Random random = new Random(20260909L);

        for (int run = 0; run < 500; run++) {
            int people = random.nextInt(8) + 2;
            Map<Long, BigDecimal> net = new LinkedHashMap<>();

            // Build a set that sums to zero: random values, with the last one absorbing
            // whatever is left over.
            BigDecimal running = BigDecimal.ZERO;
            for (int i = 1; i < people; i++) {
                BigDecimal value = BigDecimal.valueOf(random.nextInt(200_000) - 100_000, 2);
                net.put((long) i, value);
                running = running.add(value);
            }
            net.put((long) people, running.negate());

            List<Transfer> transfers = DebtSimplifier.simplify(net);

            long active = net.values().stream().filter(value -> value.signum() != 0).count();
            assertThat(transfers.size())
                    .as("payments for %d people", people)
                    .isLessThanOrEqualTo(Math.max(0, (int) active - 1));

            // Applying every transfer must leave everybody on zero.
            Map<Long, BigDecimal> after = new LinkedHashMap<>(net);
            transfers.forEach(transfer -> {
                after.merge(transfer.fromUserId(), transfer.amount(), BigDecimal::add);
                after.merge(transfer.toUserId(), transfer.amount().negate(), BigDecimal::add);
            });
            assertThat(after.values()).allSatisfy(value ->
                    assertThat(value).isEqualByComparingTo(BigDecimal.ZERO));
        }
    }
}
