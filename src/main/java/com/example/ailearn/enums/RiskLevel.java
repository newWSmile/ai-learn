package com.example.ailearn.enums;

public enum RiskLevel {

    LOW,
    MEDIUM,
    HIGH,
    UNKNOWN,
    ;


    public static boolean isValid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        try {
            RiskLevel.valueOf(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static RiskLevel from(String value) {
        if (!isValid(value)) {
            return UNKNOWN;
        }
        return RiskLevel.valueOf(value);
    }

}
