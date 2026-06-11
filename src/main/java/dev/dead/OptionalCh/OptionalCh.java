
package dev.dead.OptionalCh;

import java.util.Optional;

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
    }

    private static String getInsuranceName(Optional<Person> person) {
        return person.flatMap(Person::getCar)
                .flatMap(Car::getInsurance)
                .map(Insurance::getName)
                .orElse("Unknown");
    }

}


// A person may not own a car, so the field is declared as Optional.
class Person {
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