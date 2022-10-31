fun main() {
    var sum = 0
    var num: Int

    do {
        num = readLine()!!.toInt()
        sum += num
    } while (num != 0)

    println("Sum: $sum")
}