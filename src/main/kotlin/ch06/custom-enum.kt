enum class WeekDay2 {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;

    val lowerCaseName get() = name.lowercase()
    fun isWorkDay() = this === SATURDAY || this === SUNDAY
}

fun main() {
    println(WeekDay2.MONDAY.isWorkDay()) // false
    println(WeekDay2.WEDNESDAY.lowerCaseName) // wednesday
}