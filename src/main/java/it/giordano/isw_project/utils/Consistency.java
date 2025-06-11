package it.giordano.isw_project.utils;

import jakarta.annotation.Nullable;

public final class Consistency {

    private Consistency() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean isStrNullOrEmpty(@Nullable String str) {
        return str == null || str.trim().isEmpty();
    }
}
