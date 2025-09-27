import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FrancTest {
    @Test
    fun testFrancMultiplication() {
        val five = Franc(5)
        assertThat(five.times(2)).isEqualTo(Franc(10))
        assertThat(five.times(3)).isEqualTo(Franc(15))
    }

    @Test
    fun testEquality() {
        assertThat(Franc(5)).isEqualTo(Franc(5))
        assertThat(Franc(5)).isNotEqualTo(Franc(6))
    }
}
