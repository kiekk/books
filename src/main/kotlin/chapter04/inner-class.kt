class Outer {
    inner class Inner {
        fun getOuterReference(): Outer = this@Outer
    }
}

/*
    중첩 클래스(바깥쪽 클래스에 대한 참조를 저장하지 않음): class A
    내부 클래스(바깥쪽 클래스에 대한 참조를 저장함): inner class A
 */