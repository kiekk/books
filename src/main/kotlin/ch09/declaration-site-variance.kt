// 무공변 상태를 공변 상태로 만들기 위해서는 out 키워드를 사용
interface List2<out T> {
    val size: Int
    fun get(index: Int): T
}

class ListByArray<T>(private vararg val items: T) : List2<T> {
    override val size: Int get() = items.size
    override fun get(index: Int) = items[index]
}

fun <T> concat(list1: List2<T>, list2: List2<T>) = object : List2<T> {
    override val size: Int
        get() = list1.size + list2.size

    override fun get(index: Int): T {
        return if (index < list1.size) {
            list1.get(index)
        } else {
            list2.get(index - list1.size)
        }
    }
}

interface MutableList2<out T> : List<T> {
    // Type parameter T is declared as 'out' but occurs in 'in' position in type T
//    fun set(index: Int, value: T)
}
/*
    T 타입의 값을 입력으로 받아서 소비자(Consumer) 처럼 동작하는 set() 함수로 인해 발생하는 에러
    어떤 타입 파라미터가 항상 out 위치에서 쓰이는 경우에만 이 타입 파라미터를 공변적으로 선언할 수 있습니다.
 */

fun main() {
    val numbers = ListByArray<Number>(1, 2.5, 3f)
    val integers = ListByArray(10, 30, 30)
    val result = concat(numbers, integers) // Not Error
}