package chapter06

class Employee(val name: String, val manager: Employee?)

// 프로퍼티도 ?. 를 통해 null safe call
fun managerName(employee: Employee): String? = employee.manager?.name

fun main() {
    val ceo = Employee("Da Boss", null)
    val developer = Employee("Bob Smith", ceo)
    println(managerName(developer))
    println(managerName(ceo))
}
