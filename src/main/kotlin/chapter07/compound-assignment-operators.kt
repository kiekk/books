package chapter07

fun main() {
    // += 는 list 자체를 변경한다.
    val list = arrayListOf(1, 2)
    list += 3
    // + 는 두 리스트의 모든 원소를 포함하는 새로운 리스트를 반환한다.
    val newList = list + listOf(4, 5)
    println(list)
    println(newList)
}
