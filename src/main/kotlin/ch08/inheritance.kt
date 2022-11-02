open class Vehicle {
    init {
        println("Initializing Vehicle")
    }

    var currentSpeed = 0

    fun start() {
        println("I'm moving")
    }

    fun stop() {
        println("Stopped")
    }
}

open class FlyingVehicle : Vehicle() {
    init {
        println("Initializing FlyingVehicle")
    }

    fun takeOff() {
        println("Taking off")
    }

    fun land() {
        println("Landed")
    }
}

class Aircraft(val seats: Int) : FlyingVehicle() {
    init {
        println("Initializing Aircraft")
    }
}

fun main() {
    val aircraft = Aircraft(100)
    val vehicle: Vehicle = aircraft

    /*
    초기화 순서
    Initializing Vehicle
    Initializing FlyingVehicle
    Initializing Aircraft
     */

    vehicle.start()
    vehicle.stop()

    aircraft.start()
    aircraft.takeOff()
    aircraft.land()
    aircraft.stop()

    println(aircraft.seats)
}