package DesignPatterns.Strategy;

public class Sword implements WeaponBehavior {


    @Override
    public String useWeapon() {
        return "Attacking with a Sword!";

    }
}
