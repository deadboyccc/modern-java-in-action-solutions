package DesignPatterns.Strategy;

import java.util.stream.IntStream;

public class Main {
    static void main() {
        Player knight = new Knight(new Sword());
        knight.fight();

        Player knight2 = new Knight();
        // oh oh xD
        if (knight2.getWeaponBehavior() != null) {
            knight2.fight();
        }
        knight2.setWeaponBehavior(new Axe());
        knight2.fight();

        System.out.println("_".repeat(20));
        // testing concurrency on the fight
        IntStream.rangeClosed(1, 1000).mapToObj(num -> new Knight(new Sword(), "p" + num))
                .parallel().forEach(Player::fight);


    }
}
