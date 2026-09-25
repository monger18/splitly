package com.satyam.splitly.balance;

import com.satyam.splitly.common.money.MoneyUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Turns a set of net balances into a short list of payments that clears them.
 *
 * <p>Six people on a trip generate a tangle of small debts. Settling them one expense at
 * a time can mean dozens of transfers, most of which cancel out. Only the <em>net</em>
 * position of each person matters: if Alice is owed 600 and Bob and Carol owe 300 each,
 * two payments end it regardless of how many expenses produced those numbers.
 *
 * <p>The algorithm is greedy - repeatedly make the largest creditor and the largest debtor
 * settle as much as they can between them. With n people who have a non-zero balance it
 * produces at most n-1 transfers, because each step zeroes out at least one person.
 *
 * <p><strong>It is not guaranteed to be the true minimum.</strong> Finding that is
 * NP-hard: it means spotting every subset of people whose balances cancel exactly, which
 * is the partition problem. Greedy is fast, easy to explain, and in practice matches the
 * optimum on the group sizes this application sees. Being able to say why the optimum is
 * out of reach matters more than chasing it.
 *
 * <p>Pure and static: no Spring, no database, no clock. Every case below is a plain unit
 * test.
 */
public final class DebtSimplifier {

    private DebtSimplifier() {
    }

    /** A person and how much they are owed, or owe. */
    private record Holding(Long userId, BigDecimal amount) {
    }

    /**
     * @param netByUser positive means the group owes them, negative means they owe it.
     *                  The values must sum to zero, which they always do when derived
     *                  from expenses and settlements.
     * @return payments to make, largest first; empty when everything is already square
     */
    public static List<Transfer> simplify(Map<Long, BigDecimal> netByUser) {
        BigDecimal total = netByUser.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() != 0) {
            // Money cannot appear or vanish across a group. If this trips, the balance
            // calculation is wrong and no list of transfers could be correct.
            throw new IllegalStateException("Balances do not sum to zero: " + total);
        }

        // Largest amount first, ties broken by user id so the output is deterministic
        // and can be asserted in a test.
        Comparator<Holding> largestFirst = Comparator
                .comparing(Holding::amount, Comparator.reverseOrder())
                .thenComparing(Holding::userId);

        PriorityQueue<Holding> creditors = new PriorityQueue<>(largestFirst);
        PriorityQueue<Holding> debtors = new PriorityQueue<>(largestFirst);

        netByUser.forEach((userId, net) -> {
            if (net.signum() > 0) {
                creditors.add(new Holding(userId, net));
            } else if (net.signum() < 0) {
                debtors.add(new Holding(userId, net.negate()));   // stored as a positive size
            }
            // exactly zero: already settled, takes no part
        });

        List<Transfer> transfers = new ArrayList<>();

        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Holding creditor = creditors.poll();
            Holding debtor = debtors.poll();

            BigDecimal amount = creditor.amount().min(debtor.amount());
            transfers.add(new Transfer(debtor.userId(), creditor.userId(), MoneyUtils.normalise(amount)));

            // Whoever is left over rejoins the queue; at least one of the two is now zero,
            // which is why this terminates after at most n-1 transfers.
            BigDecimal creditorLeft = creditor.amount().subtract(amount);
            BigDecimal debtorLeft = debtor.amount().subtract(amount);

            if (creditorLeft.signum() > 0) {
                creditors.add(new Holding(creditor.userId(), creditorLeft));
            }
            if (debtorLeft.signum() > 0) {
                debtors.add(new Holding(debtor.userId(), debtorLeft));
            }
        }
        return transfers;
    }
}
