package it.giordano.isw_project.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {

    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd";

    private DateUtils(){
        throw  new IllegalStateException("Utility class");
    }

    /**
     * Analizza una stringa di data utilizzando il formato di data fornito.
     *
     * @param dateString La stringa di data da analizzare
     * @param dateFormat Il formato di data da utilizzare
     * @return Oggetto Date analizzato dalla stringa di data Jira, o null se la stringa di data è null o vuota
     * @throws ParseException Se la stringa di data non può essere analizzata
     */
    public static Date parseDate(String dateString, SimpleDateFormat dateFormat) throws ParseException {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        return dateFormat.parse(dateString);
    }

    /**
     * Formats a date according to the standard format or returns empty string.
     */
    static String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat(DateUtils.DATE_FORMAT_PATTERN);
        return dateFormat.format(date);
    }
}
