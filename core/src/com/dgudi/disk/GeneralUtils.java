package com.dgudi.disk;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class GeneralUtils {

    static DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_DATE;
    static DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME;

    static String getCurrentDate() {
        return dateFormatter.format(LocalDateTime.now(ZoneId.systemDefault()))
                .replace(":", ".").replace("-", ".");
    }

    static String getCurrentTime() {
        return timeFormatter.format(LocalDateTime.now(ZoneId.systemDefault()))
                .replace(":", ".").replace("-", ".");
    }

    static double getElapsedTime(long referenceTimeline) {
        return formatNumber((System.currentTimeMillis() - referenceTimeline) / 1000f / 60f);
    }

    static double formatNumber(double input) {
        return Math.round(input * 100.0) / 100.0;
    }

    static double convertToGigabytes(long bytes) {
        return bytes / 1024d / 1024d / 1024d;
    }

    static double convertToMegabytes(long bytes) {
        return bytes / 1024d / 1024d;
    }

    static String normaliseLength(String input, int targetLength) {
        return input + generateSpaces(targetLength - input.length());
    }

    static String generateSpaces(int quantity) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < quantity; i++) {
            stringBuilder.append(" ");
        }
        return stringBuilder.toString();
    }

    static String calculatePercentage(long all, long current) {
        String percentage_string;
        double percentage = formatNumber(current / (double) all * 100);
        if (percentage == 0) {
            percentage_string = "<.01%";
        } else {
            percentage_string = percentage + "%";
        }
        return percentage_string;
    }
}
