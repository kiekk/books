package com.example.modernjavainaction.ch11;

public class OptionalExample {

    public static void main(String[] args) {
        System.out.println(getCarInsuranceNameNullSafeV1(new Person()));
    }

    /*
    null 체크를 하지 않을 경우
    1. person 이 null 일 경우 person.getCar() 에서 NullPointerException 발생
    2. car 이 null 일 경우 car.getInsurance() 에서 NullPointerException 발생
    3. insurance 가 null 일 경우 insurance.getName() 에서 NullPointerException 발생
     */
    public static String getCarInsuranceNameNullSafeV1(Person person) {
        if (person != null) {
            Car car = person.getCar();
            if (car != null) {
                Insurance insurance = car.getInsurance();
                if (insurance != null) {
                    return insurance.getName();
                }
            }
        }
        return "Unknown";
    }
}
