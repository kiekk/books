open class Vehicle {
    var currentSpeed = 0

    fun start() {
        println("I'm moving")
    }

    fun stop() {
        println("Stopped")
    }
}

open class FlyingVehicle : Vehicle() {

    fun takeOff() {
        println("Taking off")
    }

    fun land() {
        println("Landed")
    }
}

class Aircraft(val seats: Int) : FlyingVehicle() {

}

fun main() {
    val aircraft = Aircraft(100)
    val vehicle: Vehicle = aircraft

    vehicle.start()
    vehicle.stop()

    aircraft.start()
    aircraft.takeOff()
    aircraft.land()
    aircraft.stop()

    println(aircraft.seats)
}