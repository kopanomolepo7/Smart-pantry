package com.richfield.smartpantrymanager.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Small shared helper for expiry-date math, used by PantryAdapter (to
 * show the "expiring soon" badge) and PantryFragment (to count
 * expiring-soon items for the pantry stats banner). Pulled out on its
 * own so both places share one implementation instead of duplicating
 * date-parsing logic.
 */
public final class ExpiryUtils {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private ExpiryUtils() { }

    /**
     * Returns the number of days between today and the given
     * yyyy-MM-dd expiry date (negative if already past), or null if
     * there is no valid expiry date set.
     */
    public static Long daysUntilExpiry(String expiryDate) {
        if (expiryDate == null || expiryDate.isEmpty()) {
            return null;
        }
        try {
            Date expiry = DATE_FORMAT.parse(expiryDate);
            Date today = DATE_FORMAT.parse(DATE_FORMAT.format(new Date())); // strip time component
            if (expiry == null || today == null) {
                return null;
            }
            long diffMillis = expiry.getTime() - today.getTime();
            return TimeUnit.MILLISECONDS.toDays(diffMillis);
        } catch (ParseException e) {
            return null;
        }
    }
}
