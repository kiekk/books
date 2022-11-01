interface Vehicle2 {
    val currentSpeed: Int
    fun move()
    fun stop()
}

interface FlyingVehicle2 : Vehicle2 {
    val currentHeight: Int
    fun takeOff()
    fun land()
}

class Car : Vehicle2 {
    override var currentSpeed = 0
        private set

    override fun move() {
        println("Riding...")
        currentSpeed = 50
    }

    override fun stop() {
        println("Stopped")
        currentSpeed = 0
    }
}

class Aircraft2 : FlyingVehicle2 {
    override var currentSpeed = 0
        private set

    override var currentHeight = 0
        private set

    override fun move() {
        println("Taxiing...")
        currentSpeed = 50
    }

    override fun stop() {
        println("Stopped")
        currentSpeed = 0
    }

    override fun takeOff() {
        println("Taking off...")
        currentSpeed = 500
        currentHeight = 5000
    }

    override fun land() {
        println("Landed")
        currentSpeed = 50
        currentHeight = 0
    }
}