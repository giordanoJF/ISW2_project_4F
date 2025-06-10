package it.giordano.isw_project.util;

import jakarta.annotation.Nullable;

public class Consistency {

    private Consistency() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean isStrNullOrEmpty(@Nullable String str) {
        return str == null || str.trim().isEmpty();
    }
}
