package br.com.vidasilva.jnventoryfx.validation;

import java.util.regex.Pattern;

public final class Validator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern LETTER_PATTERN = Pattern.compile(".*[A-Za-z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    private static final int MIN_PASSWORD_LENGTH = 8;

    private Validator() {
    }

    public static String requiredText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " is required.");
        }
        return value.trim();
    }

    public static String optionalText(String value, int maxLength, String fieldName) {
        if (value == null) {
            return "";
        }

        String trimmed = value.trim();
        maxLength(trimmed, maxLength, fieldName);
        return trimmed;
    }

    public static String requiredText(String value, int maxLength, String fieldName) {
        String trimmed = requiredText(value, fieldName);
        maxLength(trimmed, maxLength, fieldName);
        return trimmed;
    }

    public static String requiredEmail(String value, String fieldName) {
        String email = requiredText(value, 254, fieldName).toLowerCase();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException(fieldName + " must be a valid email address.");
        }
        return email;
    }

    public static String optionalEmail(String value, String fieldName) {
        String email = optionalText(value, 254, fieldName).toLowerCase();
        if (!email.isEmpty() && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException(fieldName + " must be a valid email address.");
        }
        return email;
    }

    public static void positiveInt(int value, String fieldName) {
        if (value <= 0) {
            throw new ValidationException(fieldName + " must be greater than zero.");
        }
    }

    public static void nonNegativeInt(int value, String fieldName) {
        if (value < 0) {
            throw new ValidationException(fieldName + " cannot be negative.");
        }
    }

    public static void nonNegativeMoney(double value, String fieldName) {
        if (value < 0) {
            throw new ValidationException(fieldName + " cannot be negative.");
        }
    }

    public static void betweenInclusive(int value, int min, int max, String fieldName) {
        if (value < min || value > max) {
            throw new ValidationException(fieldName + " must be between " + min + " and " + max + ".");
        }
    }

    public static void password(String password, String confirmPassword) {
        String checkedPassword = requiredText(password, "Password");
        String checkedConfirmPassword = requiredText(confirmPassword, "Password confirmation");

        if (!checkedPassword.equals(checkedConfirmPassword)) {
            throw new ValidationException("Passwords must match.");
        }

        if (checkedPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Password must contain at least " + MIN_PASSWORD_LENGTH + " characters.");
        }

        if (!LETTER_PATTERN.matcher(checkedPassword).matches() || !DIGIT_PATTERN.matcher(checkedPassword).matches()) {
            throw new ValidationException("Password must contain at least one letter and one number.");
        }
    }

    public static void maxLength(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            throw new ValidationException(fieldName + " must be at most " + maxLength + " characters.");
        }
    }
}
