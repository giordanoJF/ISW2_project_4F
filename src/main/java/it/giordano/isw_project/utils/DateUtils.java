package it.giordano.isw_project.utils;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.logging.Logger;

public final class DateUtils {

    @Nonnull private static final Logger LOGGER = Objects.requireNonNull(Logger.getLogger(DateUtils.class.getName()));

    private DateUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Parses a date string using the specified format.
     *
     * <p>Utility method for parsing date strings with proper null handling.</p>
     *
     * @param dateString the date string to parse
     * @param dateFormat the SimpleDateFormat to use for parsing
     * @return the parsed Date object, or null if dateString is null/empty
     * @throws ParseException if the date string cannot be parsed
     * @throws IllegalArgumentException if dateFormat is null
     */
    @Nullable
    public static Date strToDate(@Nullable String dateString, @Nullable SimpleDateFormat dateFormat)
            throws ParseException {
        if (dateFormat == null) {
            throw new IllegalArgumentException("Date format cannot be null");
        }
        if (dateString == null) {
            throw new IllegalArgumentException("Date string cannot be null");
        }
        if (dateString.trim().isEmpty()) {
            LOGGER.warning("Received empty date string, returning null");
            return null;
        }

        return dateFormat.parse(dateString);
    }
}
