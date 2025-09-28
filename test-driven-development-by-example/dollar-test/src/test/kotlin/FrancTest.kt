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
        assertThat(Money.franc(1).currency()).isEqualTo("CHF")
    }

    @Test
    fun testSimpleAddition() {
        val sum = Money.franc(5).plus(Money.franc(5))
        assertThat(sum).isEqualTo(Money.franc(10))
    }
}
