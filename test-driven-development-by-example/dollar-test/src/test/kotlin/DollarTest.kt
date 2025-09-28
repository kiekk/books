import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DollarTest {
    @Test
    fun testMultiplication() {
        val five = Money.dollar(5)
        assertThat(five.times(2)).isEqualTo(Money.dollar(10))
        assertThat(five.times(3)).isEqualTo(Money.dollar(15))
    }

    @Test
    fun testEquality() {
        assertThat(Money.dollar(5)).isEqualTo(Money.dollar(5))
        assertThat(Money.dollar(5)).isNotEqualTo(Money.dollar(6))
        assertThat(Money.dollar(5)).isNotEqualTo(Money.franc(5))
    }

    @Test
    fun testCurrency() {
        assertThat(Money.dollar(1).currency()).isEqualTo("USD")
    }

    @Test
    fun testSimpleAddition() {
        val five = Money.dollar(5)
        val sum = five.plus(five)
        val bank = Bank()
        val reduced = bank.reduce(sum, "USD")
        assertThat(reduced).isEqualTo(Money.dollar(10))
    }

    @Test
    fun testPlusReturnsSum() {
        val five = Money.dollar(5)
        val result = five.plus(five)
        val sum = result as Sum
        assertThat(sum.augend).isEqualTo(five)
        assertThat(sum.addend).isEqualTo(five)
    }

    @Test
    fun testReduceSum() {
        val sum = Sum(Money.dollar(3), Money.dollar(4))
        val bank = Bank()
        val result = bank.reduce(sum, "USD")
        assertThat(result).isEqualTo(Money.dollar(7))
    }

    @Test
    fun testReduceMoney() {
        val bank = Bank()
        val result = bank.reduce(Money.dollar(1), "USD")
        assertThat(result).isEqualTo(Money.dollar(1))
    }
}
