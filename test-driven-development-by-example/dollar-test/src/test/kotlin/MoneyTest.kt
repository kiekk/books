import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MoneyTest {
    @Test
    fun testReduceMoneyDifferentCurrency() {
        val bank = Bank()
        bank.addRate(Currency.CHF, Currency.USD, 2)
        val result = bank.reduce(Money.franc(2), Currency.USD)
        assertThat(result).isEqualTo(Money.dollar(1))
    }

    @Test
    fun testMixedAddition() {
        val fiveBucks = Money.dollar(5)
        val tenFrancs = Money.franc(10)
        val bank = Bank()
        bank.addRate(Currency.CHF, Currency.USD, 2)
        val result = bank.reduce(fiveBucks.plus(tenFrancs), Currency.USD)
        assertThat(result).isEqualTo(Money.dollar(10))
    }

    @Test
    fun testSumPlusMoney() {
        val fiveBucks = Money.dollar(5)
        val tenFrancs = Money.franc(10)
        val bank = Bank()
        bank.addRate(Currency.CHF, Currency.USD, 2)
        val sum = Sum(fiveBucks, tenFrancs).plus(fiveBucks)
        val result = bank.reduce(sum, Currency.USD)
        assertThat(result).isEqualTo(Money.dollar(15))
    }

    @Test
    fun testSumTimes() {
        val fiveBucks = Money.dollar(5)
        val tenFrancs = Money.franc(10)
        val bank = Bank()
        bank.addRate(Currency.CHF, Currency.USD, 2)
        val sum = Sum(fiveBucks, tenFrancs).times(2)
        val result = bank.reduce(sum, Currency.USD)
        assertThat(result).isEqualTo(Money.dollar(20))
    }

}