import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DollarTest {
    @Test
    fun testMultiplication() {
        val five = Dollar(5)
        five.times(2)

        assertThat(five.amount).isEqualTo(10)

        five.times(3)

        // 실패
        assertThat(five.amount).isEqualTo(15)
    }
}
