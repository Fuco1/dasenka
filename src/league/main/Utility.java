package league.main;

import java.util.Date;

/**
 * Copyright (c) 2009
 * @author Matus Goljer
 */
public class Utility {

    public static String[] tokenize(String text) {
        return text.replaceAll("^\\s+", "").replaceAll("\\s+$", "").replaceAll("\\s+", " ").split("\\s");
    }

    /**
     *
     * @param d1 Date 1
     * @param d2 Date 2
     * @return d1 - d2 in minutes
     */
    public static long getDateDiffInMinutes(Date d1, Date d2) {
        return ((d1.getTime() - d2.getTime()) / 60000);
    }

    /**
     *
     * @param d1 Date 1
     * @param d2 Date 2
     * @return d1 - d2 in seconds
     */
    public static long getDateDiffInSeconds(Date d1, Date d2) {
        return ((d1.getTime() - d2.getTime()) / 1000);
    }
}
