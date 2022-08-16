package com.example.modernjavainaction.ch11;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

public class OptionalExample {

    public static void main(String[] args) {
        System.out.println(getCarInsuranceNameNullSafeV1(new Person()));
        System.out.println(getCarInsuranceNameNullSafeV2(new Person()));
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

    /*
    V1 은 중첩 if 문으로 인해 depth 가 깊어지는 단점이 있음
    V2 는 각 if 문에서 return 처리를 하기 떄문에 너무 많은 return 처리를 하며, Unknown 문자열이 중복됨
     */
    public static String getCarInsuranceNameNullSafeV2(Person person) {
        if (person == null) {
            return "Unknown";
        }
        Car car = person.getCar();
        if (car == null) {
            return "Unknown";
        }
        Insurance insurance = car.getInsurance();
        if (insurance == null) {
            return "Unknown";
        }
        return insurance.getName();
    }

    public static String getCarInsuranceName(Optional<OptionalPerson> person) {
        return person.flatMap(OptionalPerson::getCar)
                .flatMap(OptionalCar::getInsurance)
                .map(Insurance::getName)
                .orElse("Unknown");
    }

    public static Set<String> getCarInsuranceNames(List<OptionalPerson> persons) {

        Stream<Optional<OptionalCar>> optionalStream = persons.stream()
                .map(OptionalPerson::getCar);

        Stream<Optional<Insurance>> optionalStream1 = optionalStream
                .map(optCar -> optCar.flatMap(OptionalCar::getInsurance));

        Stream<Optional<String>> optionalStream2 = optionalStream1
                .map(optInsurance -> optInsurance.map(Insurance::getName));

        Set<String> collect = optionalStream2
                .flatMap(Optional::stream)
                .collect(toSet());
        return collect;
    }

    public static Optional<Insurance> nullSafeFindCheapestInsurance(Optional<Person> person, Optional<Car> car) {
        if (person.isPresent() && car.isPresent()) {
            return Optional.of(findCheapestInsurance(person.get(), car.get()));
        } else {
            return Optional.empty();
        }
    }

    public static Insurance findCheapestInsurance(Person person, Car car) {
        // 다른 보험사에서 제공한 질의 서비스
        // 모든 데이터 비교
        Insurance cheapestCompany = new Insurance();
        return cheapestCompany;
    }

    public static Optional<Insurance> nullSafeFindCheapestInsuranceQuiz(Optional<Person> person, Optional<Car> car) {
        return person.flatMap(p -> car.map(c -> findCheapestInsurance(p, c)));
    }
}
