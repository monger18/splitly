package com.satyam.splitly.expense;

/**
 * How an expense's total is divided among its participants.
 *
 * <p>Each constant has exactly one matching
 * {@link com.satyam.splitly.expense.split.SplitStrategy} implementation. Adding a
 * new way to split means adding a constant and a strategy class - nothing else in
 * the codebase changes.
 */
public enum SplitType {
    /** Split evenly. Leftover paise are handed out one at a time, deterministically. */
    EQUAL,
    /** Caller supplies each participant's exact amount; they must sum to the total. */
    EXACT,
    /** Caller supplies percentages; they must sum to 100. */
    PERCENTAGE,
    /** Caller supplies weights, e.g. 2:1:1 for whoever ate more. */
    SHARES
}
