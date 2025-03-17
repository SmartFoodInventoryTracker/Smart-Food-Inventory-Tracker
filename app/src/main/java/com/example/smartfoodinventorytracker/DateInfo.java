package com.example.smartfoodinventorytracker;

import androidx.annotation.NonNull;

import com.google.type.DateTime;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class DateInfo {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("d/M/yyyy");

    public static int compareDates(String date1, String date2) {
        try {
            LocalDate d1 = LocalDate.parse(date1, FORMATTER);
            LocalDate d2 = LocalDate.parse(date2, FORMATTER);
            return d1.compareTo(d2);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Use day/month/year.");
        }
    }

    public static boolean isNewer(String date1, String date2) {
        return compareDates(date1, date2) > 0;
    }

    public static boolean isOlder(String date1, String date2) {
        return compareDates(date1, date2) < 0;
    }

}
