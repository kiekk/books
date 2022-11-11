package chapter06

fun printShippingLabel(person: Person) {

    // 엘비스 연산자에 값이 아니라 throw 를 사용하여 예외를 할당할 수도 있다.
    val address = person.company?.address ?: throw IllegalArgumentException("No address")
    with(address) {
        println(streetAddress)
        println("$zipCode $city, $country")
    }
}

fun main() {
    val address = Address("Elsestr. 47", 80687, "Munich", "Germany")
    val jetbrains = Company("JetBrains", address)
    val person = Person("Dmitry", jetbrains)
    printShippingLabel(person)
    printShippingLabel(Person("Alexey", null))
}