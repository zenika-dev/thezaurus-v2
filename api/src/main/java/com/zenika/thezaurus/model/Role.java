package com.zenika.thezaurus.model;

import java.util.Arrays;
import java.util.Set;

/**
 * Rôles connus de l'application. Firestore et Jackson (dé)sérialisent un enum
 * par son name() : les rôles sont donc stockés et exposés en majuscules
 * ("ADMIN", "DT", "CONSULTANT"). Les annotations @RolesAllowed utilisent les
 * constantes {@link Names} (des constantes de compilation sont requises).
 */
public enum Role {
    ADMIN,
    DT,
    CONSULTANT;

    public static final Set<String> ALL = Set.of(Names.ADMIN, Names.DT, Names.CONSULTANT);

    public static boolean isValid(String role) {
        return role != null && Arrays.stream(values()).anyMatch(r -> r.name().equals(role));
    }

    public static final class Names {
        public static final String ADMIN = "ADMIN";
        public static final String DT = "DT";
        public static final String CONSULTANT = "CONSULTANT";

        private Names() {}
    }
}
