import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MoneyTest {
    @Test
    fun testReduceMoneyDifferentCurrency() {
        val bank = Bank()
        val result = bank.reduce(Money.franc(2), "USD")
        assertThat(result).isEqualTo(Money.dollar(1))
    }
}