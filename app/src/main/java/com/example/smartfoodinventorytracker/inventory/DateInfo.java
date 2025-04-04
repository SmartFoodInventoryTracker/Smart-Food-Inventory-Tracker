package com.example.smartfoodinventorytracker.inventory;

public class DateInfo {

    public static int compareDates(String date1, String date2) {
        boolean isValidDate1 = isValidDateFormat(date1);
        boolean isValidDate2 = isValidDateFormat(date2);

        // If date1 is invalid and date2 is valid, date1 is the newest
        if (!isValidDate1 && isValidDate2) {
            return 1;  // date1 is newer
        }
        // If date2 is invalid and date1 is valid, date2 is the newest
        if (isValidDate1 && !isValidDate2) {
            return -1; // date2 is newer
        }
        // If both are invalid, they are considered equal
        if (!isValidDate1 && !isValidDate2) {
            return 0;  // Both are equally new
        }

        String[] date1parts = date1.split("/");
        int day1 = Integer.parseInt(date1parts[0]);
        int month1 = Integer.parseInt(date1parts[1]);
        int year1 = Integer.parseInt(date1parts[2]);

        String[] date2parts = date2.split("/");  // Corrected this line to use date2
        int day2 = Integer.parseInt(date2parts[0]);
        int month2 = Integer.parseInt(date2parts[1]);
        int year2 = Integer.parseInt(date2parts[2]);

        // Compare years first
        if (year1 != year2) {
            return Integer.compare(year1, year2);
        }
        // Compare months next
        if (month1 != month2) {
            return Integer.compare(month1, month2);
        }
        // Compare days last
        return Integer.compare(day1, day2);
    }

    public static boolean isValidDateFormat(String date) {
        return date.matches("\\d{1,2}/\\d{1,2}/\\d{4}");
    }

    public static boolean isNewer(String date1, String date2) {


        return compareDates(date1, date2) > 0;
    }

    public static boolean isOlder(String date1, String date2) {
        return compareDates(date1, date2) < 0;
    }

}
