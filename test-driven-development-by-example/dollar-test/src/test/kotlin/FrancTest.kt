import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FrancTest {
    @Test
    fun testFrancMultiplication() {
        val five = Money.franc(5)
        assertThat(five.times(2)).isEqualTo(Money.franc(10))
        assertThat(five.times(3)).isEqualTo(Money.franc(15))
    }

    @Test
    fun testEquality() {
        assertThat(Money.franc(5)).isEqualTo(Money.franc(5))
        assertThat(Money.franc(5)).isNotEqualTo(Money.franc(6))
    }

    @Test
    fun testCurrency() {
        assertThat(Money.franc(1).currency()).isEqualTo(Currency.CHF)
    }

    @Test
    fun testSimpleAddition() {
        val five = Money.franc(5)
        val sum = five.plus(five)
        val bank = Bank()
        val reduced = bank.reduce(sum, Currency.CHF)
        assertThat(reduced).isEqualTo(Money.franc(10))
    }

    @Test
    fun testPlusReturnsSum() {
        val five = Money.franc(5)
        val result = five.plus(five)
        val sum = result as Sum
        assertThat(sum.augend).isEqualTo(five)
        assertThat(sum.addend).isEqualTo(five)
    }

    @Test
    fun testReduceSum() {
        val sum = Sum(Money.franc(3), Money.franc(4))
        val bank = Bank()
        val result = bank.reduce(sum, Currency.CHF)
        assertThat(result).isEqualTo(Money.franc(7))
    }

    @Test
    fun testReduceMoney() {
        val bank = Bank()
        val result = bank.reduce(Money.franc(1), Currency.CHF)
        assertThat(result).isEqualTo(Money.franc(1))
    }
}
