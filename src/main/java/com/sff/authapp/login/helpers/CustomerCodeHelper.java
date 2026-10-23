package com.sff.authapp.login.helpers;

/**
 * Utility class for generating Sarpanch Fashion customer codes.
 *
 * <p>
 * Customer codes are generated from the customer's database ID.
 *
 * <pre>
 * ID          Customer Code
 * 1           SFC-0000001
 * 25          SFC-0000025
 * 125         SFC-0000125
 * 9999999     SFC-9999999
 * 10000000    SFC-10000000
 * </pre>
 *
 * <p>
 * Seven digits are used as the minimum width. If the ID becomes
 * larger than seven digits, the number is not truncated.
 */
public final class CustomerCodeHelper {

    private static final String PREFIX = "SFC-";
    private static final int MIN_DIGITS = 7;

    private CustomerCodeHelper() {
        // Utility class
    }

    /**
     * Generates a customer code from a user ID.
     *
     * @param userId customer/user database ID
     * @return generated customer code
     */
    public static String generateFromUserId(Long userId) {

        if (userId == null) {
            throw new IllegalArgumentException(
                    "User ID cannot be null"
            );
        }

        if (userId <= 0) {
            throw new IllegalArgumentException(
                    "User ID must be greater than zero"
            );
        }

        return PREFIX
                + String.format(
                "%0" + MIN_DIGITS + "d",
                userId
        );
    }

}