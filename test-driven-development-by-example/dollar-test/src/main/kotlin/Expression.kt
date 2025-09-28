interface Expression {
    fun reduce(bank: Bank, to: Currency): Money

    fun plus(addend: Expression): Expression

    fun times(multiplier: Int): Expression
}
