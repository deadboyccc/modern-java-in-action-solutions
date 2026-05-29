package DesignPatterns.Strategy;

public class Axe implements WeaponBehavior {

    @Override
    public String useWeapon() {
        return "Swinging an Axe!";

    }
}
