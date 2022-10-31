fun main() {
    println("for loop no step")
    for (num in 0..10) {
        print(num)
        print(" ")
    }
    println()
    println("for loop step 2")
    for (num in 0..10 step 2) {
        print(num)
        print(" ")
    }
    println()
    println("for loop step 2 downTo 1")
    for (num in 10 downTo 1 step 2) {
        print(num)
        print(" ")
    }
}