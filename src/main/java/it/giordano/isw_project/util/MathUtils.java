package it.giordano.isw_project.util;

public class MathUtils {

    private MathUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Calculates a percentage.
     *
     * @param part The part
     * @param total The total
     * @return The percentage
     */
    public static double calculatePercentage(int part, int total) {
        return part * 100.0 / total;
    }
}
