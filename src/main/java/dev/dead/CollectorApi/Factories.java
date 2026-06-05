package dev.dead.CollectorApi;

import java.security.*;
import java.util.*;

import static java.util.Map.entry;

public class Factories {

    public static void main(String[] args) {
        // 1. Immutable and Mutable Collections demo
        var list = List.of(1, 2, 3);
        var map = Map.ofEntries(
                entry(1, "one"),
                entry(2, "two")
        );
        var set = Set.of(1, 2, 3);

        var mutable = new ArrayList<>(list);
        mutable.removeIf(integer -> integer > 2);
        mutable.forEach(System.out::println);

        mutable.replaceAll(integer -> integer + 100);
        mutable.forEach(System.out::println);

        // 2. Sorting Map Entries
        Map<String, String> favouriteMovies = Map.ofEntries(
                entry("Raphael", "Star Wars"),
                entry("Cristina", "Matrix"),
                entry("Olivia", "James Bond")
        );

        favouriteMovies.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(System.out::println);

        // 3. Cleaner multi-value Map population using computeIfAbsent
        Map<String, List<String>> stringToStringList = new HashMap<>();

        stringToStringList.computeIfAbsent("A", _ -> new ArrayList<>()).addAll(List.of("hello", "hi"));
        stringToStringList.computeIfAbsent("B", _ -> new ArrayList<>()).addAll(List.of("good Bye", "bye"));

        for (var entry : stringToStringList.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }

        // 4. Hashing data with handled exceptions inside the function
        var dataToHash = new HashMap<String, byte[]>();
        var strToBeHashed = List.of("Hello", "hi", "goodbye", "bye");

        strToBeHashed.forEach(string -> dataToHash.computeIfAbsent(string, Factories::getHash));

        // Quick verification of the hash map
        dataToHash.forEach((key, value) -> System.out.println(key + " -> Hash : " + HexFormat.of().formatHex(value)));

        var rsaPair = generateRSAPair("password");
        System.out.println(HexFormat.of().formatHex(rsaPair.getPublic().getEncoded()));
        System.out.println(HexFormat.of().formatHex(rsaPair.getPrivate().getEncoded()));
    }

    private static byte[] getHash(String s) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(s.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not found", e);
        }
    }

    private static KeyPair generateRSAPair(String s) {

        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1024);

            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();
            return new KeyPair(publicKey, privateKey);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not found", e);
        }
    }
}
