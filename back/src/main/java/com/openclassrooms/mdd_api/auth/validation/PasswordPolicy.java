package com.openclassrooms.mdd_api.auth.validation;

public final class PasswordPolicy {
    private PasswordPolicy() {}

    public static boolean isValid(String password) {
        if (password == null || password.length() < 8) return false;

        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));

        return hasLower && hasUpper && hasDigit && hasSpecial;
    }
}

