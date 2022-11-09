package chapter05

class Book(val title: String, val authors: List<String>)

fun main() {
    val strings = listOf("abc", "def")
    println(strings.flatMap { it.toList() })

    val books = listOf(
        Book("Thursday Next", listOf("Jasper Fforde")),
        Book("Mort", listOf("Terry Pratchett")),
        Book("Good Omens", listOf("Terry Pratchett", "Neil Gaiman"))
    )
    println(books.flatMap { it.authors }.toSet())

    // 중첩 list 들을 변환 없이 단순히 펼치기만 한다면 flatten 을 사용하면 된다.
    val numbers = listOf(listOf(1, 2, 3), listOf(5, 6, 7), listOf(8, 9, 0))
    val result = numbers.flatten()
    println(result)
}