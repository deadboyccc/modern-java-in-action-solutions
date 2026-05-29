package DesignPatterns.Strategy;

public class Knight extends Player {

    Knight() {
    }


    Knight(WeaponBehavior wp) {
        super(wp);
    }

    Knight(WeaponBehavior wp, String name) {
        super(wp, name);
    }

    @Override
    void fight() {
        System.out.println(getName() + "->" + getWeaponBehavior().useWeapon());
    }
}
