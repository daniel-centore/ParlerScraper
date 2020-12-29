package com.danielcentore.scraper.parler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

import com.danielcentore.scraper.parler.api.components.ScrapedRange;

public class PUtils {

    /**
     * Converts numbers provided by the Parler API to regular integers
     * 
     * @param parler number, e.g. "3.5m"
     * @return regular number, e.g. 3500000
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

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Based on https://www.geeksforgeeks.org/merging-intervals/
    public static List<TimeInterval> mergeScrapedRanges(List<ScrapedRange> scrapedRanges) {
        List<TimeInterval> ranges = new ArrayList<>();
        for (ScrapedRange r : scrapedRanges) {
            ranges.add(new TimeInterval(
                    r.getStartParlerTime().toUnixTimeMs(),
                    r.getEndParlerTime().toUnixTimeMs()));
        }

        // Test if the given set has at least one interval  
        if (ranges.size() <= 0)
            return new ArrayList<>();

        // Create an empty stack of intervals  
        Stack<TimeInterval> stack = new Stack<>();
        
        // sort the intervals in increasing order of start time  
        Collections.sort(ranges, new Comparator<TimeInterval>() {
            @Override
            public int compare(TimeInterval i1, TimeInterval i2) {
                return Long.compare(i1.min, i2.min);
            }
        });

        // push the first interval to stack  
        stack.push(ranges.get(0));

        // Start from the next interval and merge if necessary  
        for (int i = 1; i < ranges.size(); i++) {
            // get interval from stack top  
            TimeInterval top = stack.peek();

            // if current interval is not overlapping with stack top,  
            // push it to the stack  
            if (top.max < ranges.get(i).min)
                stack.push(ranges.get(i));

            // Otherwise update the ending time of top if ending of current  
            // interval is more  
            else if (top.max < ranges.get(i).max) {
                top.max = ranges.get(i).max;
                stack.pop();
                stack.push(top);
            }
        }

        return new ArrayList<>(stack);
    }

}

class TimeInterval {
    public long min;
    public long max;

    public TimeInterval(long a, long b) {
        if (a < b) {
            this.min = a;
            this.max = b;
        } else {
            this.min = b;
            this.max = a;
        }
    }

    @Override
    public String toString() {
        return "TimeInterval [min=" + min + ", max=" + max + "]";
    }
}
