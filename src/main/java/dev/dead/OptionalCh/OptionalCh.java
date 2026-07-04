
package dev.dead.OptionalCh;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class OptionalCh {
    static void main() {
        var p1 = new Person();
        p1.setCar(new Car());
        p1.getCar().ifPresent(car -> car.setInsurance(new Insurance("Insurance Inc")));

        // ----------
        var car = p1.getCar();
        var insurance = car.flatMap(Car::getInsurance);
        var companyName = insurance.map(Insurance::getName);
        companyName.ifPresent(System.out::println);

        // ------
        // from person to insurance Company Name
        var cmName = p1.getCar()
                .flatMap(Car::getInsurance)
                .map(Insurance::getName)
                .orElse("Unknown");


        // through a method
        System.out.println(getInsuranceName(Optional.of(p1)));
        var map = new HashMap<Object, String>();
        map.put("key", "value");
        Optional<Object> value = Optional.ofNullable(map.get("key"));
    }

    public static Optional<Integer> stringToInt(String s) {
        try {
            return Optional.of(Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static String getInsuranceName(Optional<Person> person) {
        return person.flatMap(Person::getCar)
                .flatMap(Car::getInsurance)
                .map(Insurance::getName)
                .orElse("Unknown");
    }

    public Set<String> getCarInsuranceNames(List<Person> persons) {
        return persons.stream()
                // 1. Transform Stream<Person> into Stream<Optional<Car>>
                .map(Person::getCar)

                // 2. Unwrap Optional<Car> into Stream<Car> (automatically drops empty optionals)
                .flatMap(Optional::stream)

                // 3. Transform Stream<Car> into Stream<Optional<Insurance>>
                .map(Car::getInsurance)

                // 4. Unwrap Optional<Insurance> into Stream<Insurance>
                .flatMap(Optional::stream)

                // 5. Transform Stream<Insurance> into Stream<String> (names)
                .map(Insurance::getName)

                // 6. Collect distinct names into a Set
                .collect(toSet());

    }

    public String getCarInsuranceName(Optional<Person> person, int minAge) {
        return person.filter(p -> p.getAge() >= minAge)
                .flatMap(Person::getCar)
                .flatMap(Car::getInsurance)
                .map(Insurance::getName)
                .orElse("Unknown");
    }

    public Optional<Insurance> nullSafeFindCheapestInsurance(
            Optional<Person> person, Optional<Car> car) {
        return person.flatMap(p -> car.map(c -> findCheapestInsurance(p, c)));
    }

    private Insurance findCheapestInsurance(Person p, Car c) {
        // mock
        return null;
    }
}

// A person may not own a car, so the field is declared as Optional.
class Person {
    private int age;

    public int getAge() {
        return age;
    }

    private Optional<Car> car = Optional.empty();

    public Optional<Car> getCar() {
        return car;
    }

    public void setCar(Car car) {
        this.car = Optional.of(car);
    }
}

// A car may not be insured, so the field is declared as Optional.
class Car {
    private Optional<Insurance> insurance = Optional.empty();

    public Optional<Insurance> getInsurance() {
        return insurance;
    }

    public void setInsurance(Insurance insurance) {
        this.insurance = Optional.of(insurance);
    }

}

// An insurance company must have a name, so it is a plain String.
class Insurance {
    private final String name;

    Insurance(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}