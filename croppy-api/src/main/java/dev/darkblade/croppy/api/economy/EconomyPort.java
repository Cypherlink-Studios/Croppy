package dev.darkblade.croppy.api.economy;

import java.util.UUID;

/**
 * Port contract for querying balances, executing withdrawals/deposits, and formatting currencies.
 */
public interface EconomyPort {

    /**
     * Verifies whether the specified player has at least the designated currency amount.
     *
     * @param playerId the unique identifier of the player (cannot be null)
     * @param amount   the currency amount to evaluate (must be non-negative)
     * @return true if the balance meets or exceeds the amount, false otherwise
     */
    boolean has(UUID playerId, double amount);

    /**
     * Withdraws the specified currency amount from the player's balance.
     *
     * @param playerId the unique identifier of the player (cannot be null)
     * @param amount   the currency amount to withdraw (must be positive)
     * @return true if successfully deducted, false if insufficient funds or rejected
     */
    boolean withdraw(UUID playerId, double amount);

    /**
     * Deposits the specified currency amount into the player's balance.
     *
     * @param playerId the unique identifier of the player (cannot be null)
     * @param amount   the currency amount to deposit (must be positive)
     * @return true if successfully deposited, false otherwise
     */
    boolean deposit(UUID playerId, double amount);

    /**
     * Formats a raw numeric monetary amount into a localized currency string.
     *
     * @param amount the currency amount
     * @return formatted currency representation
     */
    String format(double amount);
}
