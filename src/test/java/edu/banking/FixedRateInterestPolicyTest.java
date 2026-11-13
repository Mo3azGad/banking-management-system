package edu.banking;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class FixedRateInterestPolicyTest {
    @ParameterizedTest
    @CsvSource({"0.001, 1000.00, 1.00", "0.003, 1000.00, 3.00", "0, 1000.00, 0.00",
                "0.001, 0.00, 0.00", "0.001, 0.01, 0.00", "1, 100.00, 100.00",
                "1.23456789, 100.00, 123.46"})
    void appliesTheMonthlyDecimalRateWithoutAnnualizationOrInventedRateCap(BigDecimal rate,
                                                                         BigDecimal balance, BigDecimal expected) {
        InterestPolicy policy = new FixedRateInterestPolicy(rate);
        assertEquals(expected, policy.calculate(balance));
    }

    @Test
    void rejectsMissingRateWithAClearDomainException() {
        var error = assertThrows(InvalidBankingInputException.class, () -> new FixedRateInterestPolicy(null));
        assertEquals("Monthly interest rate must not be null.", error.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.001", "-1", "-0.000000000000001"})
    void rejectsNegativeMonthlyRates(String rate) {
        var error = assertThrows(InvalidBankingInputException.class,
                () -> new FixedRateInterestPolicy(new BigDecimal(rate)));
        assertEquals("Monthly interest rate must not be negative.", error.getMessage());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"-0.01", "-100", "1.001", "0.0001"})
    void rejectsMissingNegativeOrSubCentBalances(String value) {
        InterestPolicy policy = new FixedRateInterestPolicy(new BigDecimal("0.003"));
        BigDecimal balance = value == null ? null : new BigDecimal(value);
        assertThrows(InvalidBankingInputException.class, () -> policy.calculate(balance));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"-1", "0.001"})
    void zeroRateDoesNotBypassBalanceValidation(String value) {
        InterestPolicy policy = new FixedRateInterestPolicy(BigDecimal.ZERO);
        BigDecimal balance = value == null ? null : new BigDecimal(value);
        assertThrows(InvalidBankingInputException.class, () -> policy.calculate(balance));
    }

    @ParameterizedTest
    @CsvSource({"5.00, 0.00", "15.00, 0.02", "25.00, 0.02", "35.00, 0.04",
                "1005.00, 1.00", "1015.00, 1.02", "4.99, 0.00", "5.01, 0.01",
                "14.99, 0.01", "15.01, 0.02"})
    void halfEvenRoundingHandlesBothTieDirectionsAndAdjacentValues(BigDecimal balance, BigDecimal expected) {
        InterestPolicy policy = new FixedRateInterestPolicy(new BigDecimal("0.001"));
        assertEquals(expected, policy.calculate(balance));
    }

    @ParameterizedTest
    @CsvSource({"0.00004999999999, 0.00", "0.00005000000001, 0.01", "0.00123456789, 0.12"})
    void retainsRatePrecisionUntilTheFinalInterestRounding(BigDecimal rate, BigDecimal expected) {
        InterestPolicy policy = new FixedRateInterestPolicy(rate);
        assertEquals(expected, policy.calculate(new BigDecimal("100.00")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"100", "100.0000", "1E+2"})
    void acceptsEquivalentExactCentRepresentationsAndNormalizesTheResult(String balance) {
        InterestPolicy policy = new FixedRateInterestPolicy(new BigDecimal("0.003"));
        assertEquals(new BigDecimal("0.30"), policy.calculate(new BigDecimal(balance)));
    }

    @Test
    void veryLargeBalanceCalculatesWithoutFloatingPointPrecisionLoss() {
        InterestPolicy policy = new FixedRateInterestPolicy(new BigDecimal("0.001"));
        BigDecimal balance = new BigDecimal("123456789012345678901234567890.12");
        assertEquals(new BigDecimal("123456789012345678901234567.89"), policy.calculate(balance));
    }

    @Test
    void rateTrailingZerosDoNotChangeInterestValue() {
        InterestPolicy policy = new FixedRateInterestPolicy(new BigDecimal("0.003000000"));
        assertEquals(new BigDecimal("0.30"), policy.calculate(new BigDecimal("100.00")));
    }

    @Test
    void repeatedCalculationsDoNotAccumulateOrChangeTheFixedRate() {
        InterestPolicy policy = new FixedRateInterestPolicy(new BigDecimal("0.003"));
        BigDecimal balance = new BigDecimal("100.00");
        BigDecimal first = policy.calculate(balance);
        policy.calculate(new BigDecimal("200.00"));
        assertAll(() -> assertEquals(new BigDecimal("0.30"), first),
                  () -> assertEquals(first, policy.calculate(balance)),
                  () -> assertEquals(new BigDecimal("100.00"), balance));
    }

    @Test
    void unsupportedDecimalScaleIsReportedAsACustomException() {
        BigDecimal tinyRate = new BigDecimal(BigInteger.ONE, Integer.MAX_VALUE);
        InterestPolicy policy = new FixedRateInterestPolicy(tinyRate);
        var error = assertThrows(InvalidBankingInputException.class,
                () -> policy.calculate(new BigDecimal("1.00")));
        assertEquals("Interest calculation exceeds supported decimal precision.", error.getMessage());
    }

    @Test
    void actualCheckingAndSavingsRateMetadataProduceDifferentQuotes() {
        Bank bank = new Bank();
        bank.registerCustomer("C1", "Ada");
        LocalDate date = LocalDate.of(2024, 2, 29);
        Account checking = bank.openCheckingAccount("C", "C1", new BigDecimal("100"), date);
        Account savings = bank.openSavingsAccount("S", "C1", new BigDecimal("100"), date);
        InterestPolicy checkingPolicy = new FixedRateInterestPolicy(checking.getMonthlyInterestRate());
        InterestPolicy savingsPolicy = new FixedRateInterestPolicy(savings.getMonthlyInterestRate());
        assertAll(() -> assertEquals(new BigDecimal("0.10"), checkingPolicy.calculate(checking.getBalance())),
                  () -> assertEquals(new BigDecimal("0.30"), savingsPolicy.calculate(savings.getBalance())));
    }

    @Test
    void calculationDoesNotPostInterestOrMutateEvenAFrozenAccountsLedger() {
        Bank bank = new Bank();
        bank.registerCustomer("C1", "Ada");
        Account account = bank.openSavingsAccount("S", "C1", new BigDecimal("100"), LocalDate.of(2024, 2, 29));
        account.freeze();
        List<Transaction> before = account.getTransactionHistory();
        InterestPolicy policy = new FixedRateInterestPolicy(account.getMonthlyInterestRate());
        BigDecimal result = policy.calculate(account.getBalance());
        assertAll(() -> assertEquals(new BigDecimal("0.30"), result),
                  () -> assertEquals(new BigDecimal("100.00"), account.getBalance()),
                  () -> assertEquals(before, account.getTransactionHistory()),
                  () -> assertTrue(account.isFrozen()));
    }
}
