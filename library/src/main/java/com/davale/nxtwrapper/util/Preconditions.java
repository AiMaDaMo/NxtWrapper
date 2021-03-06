package com.davale.nxtwrapper.util;

import android.os.Looper;

/**
 * Helper class to ensure that passed objects met the condition which they are checked for,
 * like non-null checks.
 *
 * @author Alex Lardschneider
 */
public final class Preconditions {

    private Preconditions() {
    }

    /**
     * Ensures that an object reference passed as a parameter to the calling method is not null.
     *
     * @param reference    an object reference
     * @param errorMessage the exception message to use if the check fails; will be converted to a
     *                     string using {@link String#valueOf(Object)}
     * @return the non-null reference that was validated
     * @throws NullPointerException if {@code reference} is null
     */
    public static <T> T checkNotNull(T reference, Object errorMessage) {
        if (reference == null) {
            throw new NullPointerException(String.valueOf(errorMessage));
        }
        return reference;
    }

    public static void checkNotUiThread() {
        //noinspection ObjectEquality
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("Cannot be executed on UI thread");
        }
    }
}