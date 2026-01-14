package dev.vepo.passport.user;

import java.security.SecureRandom;
import java.util.Random;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PasswordGenerator {

    private final int length;
    private final Random random;

    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL_CHARS = "!@#$%^&*()-_=+[]{}|;:,.<>?";
    private static final String ALL_CHARS = UPPERCASE + LOWERCASE + DIGITS + SPECIAL_CHARS;

    public PasswordGenerator(@ConfigProperty(name = "password.generator.length") int length) {
        this.length = length;
        this.random = new SecureRandom();
    }

    public String generate() {
        if (length < 4) {
            throw new IllegalArgumentException("Password length must be at least 4 characters to include all character types");
        }

        char[] password = new char[length];

        // Ensure at least one character from each category
        password[0] = UPPERCASE.charAt(random.nextInt(UPPERCASE.length()));
        password[1] = LOWERCASE.charAt(random.nextInt(LOWERCASE.length()));
        password[2] = DIGITS.charAt(random.nextInt(DIGITS.length()));
        password[3] = SPECIAL_CHARS.charAt(random.nextInt(SPECIAL_CHARS.length()));

        // Fill the rest of the password with random characters from all categories
        for (int i = 4; i < length; i++) {
            password[i] = ALL_CHARS.charAt(random.nextInt(ALL_CHARS.length()));
        }

        // Shuffle the characters to avoid predictable pattern
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(length);
            char temp = password[i];
            password[i] = password[randomIndex];
            password[randomIndex] = temp;
        }

        return new String(password);
    }
}