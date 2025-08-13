package com.mediroute.utils;

public final class LogSanitizer {
    private LogSanitizer() {}

    public static String sensitive(String value) {
        if (value == null || value.isBlank()) return "";
        return "***";
    }

    public static String sanitizeEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];
        String maskedLocal = local.length() <= 2 ? "**" : local.substring(0, 2) + "***";
        return maskedLocal + "@" + domain;
    }

    public static String sanitizePhone(String phone) {
        if (phone == null) return "***";
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() <= 4) return "***";
        return "***" + digits.substring(digits.length() - 4);
    }
}


