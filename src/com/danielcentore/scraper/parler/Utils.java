package com.danielcentore.scraper.parler;

import java.math.BigDecimal;

public class Utils {

    /**
     * Converts numbers provided by the Parler API to regular integers
     * @param number e.g. "3.5m"
     * @return
     */
    public static Long deparlify(String number) {
        if (number == null || number.isEmpty()) {
            return null;
        }
        try {
            number = number.toLowerCase();
    
            char suffix = number.charAt(number.length() - 1);
            if (Character.isDigit(suffix)) {
                return Long.parseLong(number);
            }
            BigDecimal base = new BigDecimal(number.substring(0, number.length() - 1));
            int multiplier = 0;
            switch (suffix) {
            case 'k':
                multiplier = 1000;
                break;
            case 'm':
                multiplier = 1000000;
                break;
            case 'b':
                multiplier = 1000000000;
                break;
            }
    
            return base.multiply(new BigDecimal(multiplier)).longValue();
        } catch (NumberFormatException e) {
            throw new RuntimeException("INVALID PARLER NUMBER FORMAT: " + number, e);
        }
    }

}
