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
        val sum = Money.dollar(5).plus(Money.dollar(5))
        assertThat(sum).isEqualTo(Money.dollar(10))
    }
}
