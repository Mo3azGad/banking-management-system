package edu.banking;

import java.math.BigDecimal;

/** A pure monthly interest calculation, separate from dated ledger posting.
 * Implementations accept a nonnull, nonnegative exact-cent balance and return nonnegative
 * interest at scale 2. They must reject unsupported input with a BankingException and must
 * not mutate an account or read the system clock. No month eligibility is decided here.
 */
@FunctionalInterface
public interface InterestPolicy {
    BigDecimal calculate(BigDecimal balance);
}
