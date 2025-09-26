import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DollarTest {
    @Test
    fun testMultiplication() {
        val five = Dollar(5)
        var product = five.times(2)
        assertThat(product.amount).isEqualTo(10)

        product = five.times(3)
        assertThat(product.amount).isEqualTo(15)
    }

    @Test
    fun testEquality() {
        assertThat(Dollar(5)).isEqualTo(Dollar(5))
        assertThat(Dollar(5)).isNotEqualTo(Dollar(6))
    }
}
