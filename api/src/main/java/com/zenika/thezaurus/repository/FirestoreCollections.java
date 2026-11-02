package com.zenika.thezaurus.repository;

import java.util.Optional;

/** Collection names are independent: never derive one collection by rewriting another's path. */
final class FirestoreCollections {
    static String talks(Optional<String> prefix) {
        return name(prefix, "talks");
    }

    static String conferences(Optional<String> prefix) {
        return name(prefix, "conferences");
    }

    private static String name(Optional<String> prefix, String collection) {
        return Optional.ofNullable(prefix)
                .flatMap(value -> value)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value + "_" + collection)
                .orElse(collection);
    }

    private FirestoreCollections() {}
}
