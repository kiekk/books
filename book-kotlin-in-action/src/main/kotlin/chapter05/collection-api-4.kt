package chapter05

fun main() {
    val people = listOf(
        Person("Alice", 31),
        Person("Bob", 29), Person("Carol", 31)
    )
    println(people.groupBy { it.age })
    // {31=[Person(name=Alice, age=31), Person(name=Carol, age=31)], 29=[Person(name=Bob, age=29)]}
    // groupBy 에 사용된 field 가 key 가 되며, 값에 각 요소들이 매핑됨

    val list = listOf("a", "ab", "b")
    println(list.groupBy(String::first))
    // 멤버 참조 사용, 문자열의 첫 글자에 따라 그룹핑
}
