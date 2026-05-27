package DesignPatterns;

abstract public class Player {
    private String name;
    private WeaponBehavior weaponBehavior;

    Player() {
    }

    Player(WeaponBehavior wp) {
        this.weaponBehavior = wp;
    }

    Player(WeaponBehavior wp, String name) {
        this.weaponBehavior = wp;
        this.name = name;
    }

    abstract void fight();

    public WeaponBehavior getWeaponBehavior() {
        return weaponBehavior;
    }

    public void setWeaponBehavior(WeaponBehavior weaponBehavior) {
        this.weaponBehavior = weaponBehavior;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
