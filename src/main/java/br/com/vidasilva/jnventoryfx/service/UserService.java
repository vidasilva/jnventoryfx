package br.com.vidasilva.jnventoryfx.service;

import br.com.vidasilva.jnventoryfx.model.User;
import br.com.vidasilva.jnventoryfx.model.UserRole;
import br.com.vidasilva.jnventoryfx.repository.UserRepository;
import br.com.vidasilva.jnventoryfx.security.AuthorizationService;
import br.com.vidasilva.jnventoryfx.security.PasswordHasher;
import br.com.vidasilva.jnventoryfx.security.Permission;
import br.com.vidasilva.jnventoryfx.validation.Validator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

public class UserService {

    private static final UserRepository USER_REPOSITORY = new UserRepository();
    private static final ObservableList<User> USERS = FXCollections.observableArrayList(USER_REPOSITORY.findAll());
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String TEMP_PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
    private static final String RESET_CODE_ALPHABET = "0123456789";
    private static final int TEMP_PASSWORD_LENGTH = 14;
    private static final int RESET_CODE_LENGTH = 8;
    private static final Duration RESET_CODE_TTL = Duration.ofMinutes(15);
    private final EmailService emailService = new EmailService();

    public ObservableList<User> getUsers() {
        return USERS;
    }

    public boolean registerUser(String username, String email, String password, String confirmPassword) {
        return registerUserInternal(username, email, password, confirmPassword, UserRole.CASHIER, false) != null;
    }

    public boolean createUser(String username, String email, String password, UserRole role) {
        AuthorizationService.require(Permission.MANAGE_USERS);
        return registerUserInternal(username, email, password, password, role, true) != null;
    }

    public UserCreationResult createUserWithTemporaryPassword(String username, String email, UserRole role) {
        AuthorizationService.require(Permission.MANAGE_USERS);

        String temporaryPassword = generateTemporaryPassword();
        User user = registerUserInternal(username, email, temporaryPassword, temporaryPassword, role, true);

        if (user == null) {
            return null;
        }

        AuditService.record(
                "CREATE_USER",
                "USER",
                user.getEmail(),
                "SUCCESS",
                "Created user " + user.getUsername() + " with role " + user.getRole().name() + " and a temporary password."
        );

        return new UserCreationResult(user, temporaryPassword);
    }

    public PasswordResetRequestResult requestPasswordReset(String email) {
        return requestPasswordResetInternal(email, false);
    }

    public PasswordResetRequestResult sendPasswordResetEmailForUser(String email) {
        AuthorizationService.require(Permission.MANAGE_USERS);
        return requestPasswordResetInternal(email, true);
    }

    public boolean resetPasswordWithRecoveryCode(String email, String resetCode, String newPassword, String confirmPassword) {
        String normalizedEmail = Validator.requiredEmail(email, "Email");
        String normalizedCode = Validator.requiredText(resetCode, 32, "Reset code").replaceAll("\\s+", "");
        Validator.password(newPassword, confirmPassword);

        User user = USER_REPOSITORY.findByEmail(normalizedEmail);

        if (user == null) {
            AuditService.recordAnonymous(normalizedEmail, "PASSWORD_RECOVERY_COMPLETE", "USER", normalizedEmail, "FAILURE", "Unknown user email.");
            throw new IllegalArgumentException("Invalid or expired reset code.");
        }

        UserRepository.PasswordResetToken token = USER_REPOSITORY.findPasswordResetToken(normalizedEmail);

        if (token == null || token.expiresAt() == null || token.expiresAt().isBlank()) {
            AuditService.recordAnonymous(normalizedEmail, "PASSWORD_RECOVERY_COMPLETE", "USER", normalizedEmail, "FAILURE", "No active reset token.");
            throw new IllegalArgumentException("Invalid or expired reset code.");
        }

        Instant expiresAt = Instant.parse(token.expiresAt());

        if (Instant.now().isAfter(expiresAt)) {
            USER_REPOSITORY.clearPasswordResetToken(normalizedEmail);
            AuditService.recordAnonymous(normalizedEmail, "PASSWORD_RECOVERY_COMPLETE", "USER", normalizedEmail, "FAILURE", "Expired reset token.");
            throw new IllegalArgumentException("Invalid or expired reset code.");
        }

        if (!PasswordHasher.verify(normalizedCode, token.tokenHash())) {
            AuditService.recordAnonymous(normalizedEmail, "PASSWORD_RECOVERY_COMPLETE", "USER", normalizedEmail, "FAILURE", "Invalid reset token.");
            throw new IllegalArgumentException("Invalid or expired reset code.");
        }

        if (PasswordHasher.verify(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("New password cannot be the same as the current password.");
        }

        String newPasswordHash = PasswordHasher.hash(newPassword);
        USER_REPOSITORY.updatePasswordHash(user.getEmail(), newPasswordHash, false);
        USER_REPOSITORY.clearPasswordResetToken(user.getEmail());
        user.setPasswordHash(newPasswordHash);
        user.setMustChangePassword(false);
        updateCachedPassword(user.getEmail(), newPasswordHash, false);

        AuditService.recordForUser(user, "PASSWORD_RECOVERY_COMPLETE", "USER", user.getEmail(), "SUCCESS", "User reset password with an emailed recovery code.");
        return true;
    }

    public boolean signIn(String email, String password) {
        return authenticateUser(email, password) != null;
    }

    public User authenticateUser(String email, String password) {
        if (isBlank(email) || isBlank(password)) {
            AuditService.recordAnonymous(email, "SIGN_IN", "USER", safe(email), "FAILURE", "Missing email or password.");
            return null;
        }

        String normalizedEmail = normalizeEmail(email);
        User user = USER_REPOSITORY.findByEmail(normalizedEmail);

        if (user == null) {
            AuditService.recordAnonymous(normalizedEmail, "SIGN_IN", "USER", normalizedEmail, "FAILURE", "Invalid credentials.");
            return null;
        }

        String storedPasswordHash = user.getPasswordHash();

        if (PasswordHasher.verify(password, storedPasswordHash)) {
            AuditService.recordForUser(user, "SIGN_IN", "USER", user.getEmail(), "SUCCESS", "User authenticated.");
            return user;
        }

        if (!PasswordHasher.isHashed(storedPasswordHash) && password.equals(storedPasswordHash)) {
            String upgradedHash = PasswordHasher.hash(password);
            USER_REPOSITORY.updatePasswordHash(user.getEmail(), upgradedHash);
            user.setPasswordHash(upgradedHash);
            updateCachedPasswordHash(user.getEmail(), upgradedHash);
            AuditService.recordForUser(user, "SIGN_IN", "USER", user.getEmail(), "SUCCESS", "User authenticated and legacy password was migrated to a hash.");
            return user;
        }

        AuditService.recordAnonymous(normalizedEmail, "SIGN_IN", "USER", normalizedEmail, "FAILURE", "Invalid credentials.");
        return null;
    }

    public boolean changePasswordAfterFirstLogin(User user, String newPassword, String confirmPassword) {
        if (user == null) {
            return false;
        }

        Validator.password(newPassword, confirmPassword);

        if (PasswordHasher.verify(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("New password cannot be the same as the temporary password.");
        }

        String newPasswordHash = PasswordHasher.hash(newPassword);
        USER_REPOSITORY.updatePasswordHash(user.getEmail(), newPasswordHash, false);
        user.setPasswordHash(newPasswordHash);
        user.setMustChangePassword(false);
        updateCachedPassword(user.getEmail(), newPasswordHash, false);

        AuditService.recordForUser(user, "CHANGE_PASSWORD", "USER", user.getEmail(), "SUCCESS", "User replaced a temporary password.");
        return true;
    }

    private PasswordResetRequestResult requestPasswordResetInternal(String email, boolean requestedByAdmin) {
        String normalizedEmail = Validator.requiredEmail(email, "Email");
        User user = USER_REPOSITORY.findByEmail(normalizedEmail);
        Instant expiresAt = Instant.now().plus(RESET_CODE_TTL);

        if (user == null) {
            AuditService.recordAnonymous(
                    normalizedEmail,
                    requestedByAdmin ? "ADMIN_PASSWORD_RECOVERY_REQUEST" : "PASSWORD_RECOVERY_REQUEST",
                    "USER",
                    normalizedEmail,
                    "FAILURE",
                    "Password reset requested for unknown email."
            );
            return new PasswordResetRequestResult(normalizedEmail, expiresAt, false, null);
        }

        String resetCode = generateResetCode();
        USER_REPOSITORY.savePasswordResetToken(user.getEmail(), PasswordHasher.hash(resetCode), expiresAt.toString());

        EmailService.PasswordResetMailResult deliveryResult = emailService.sendPasswordResetCode(user.getEmail(), resetCode, expiresAt);

        AuditService.record(
                requestedByAdmin ? "ADMIN_PASSWORD_RECOVERY_REQUEST" : "PASSWORD_RECOVERY_REQUEST",
                "USER",
                user.getEmail(),
                "SUCCESS",
                deliveryResult.sentBySmtp()
                        ? "Password reset code sent by SMTP email."
                        : "Password reset code written to development email outbox."
        );

        return new PasswordResetRequestResult(
                user.getEmail(),
                expiresAt,
                deliveryResult.sentBySmtp(),
                deliveryResult.developmentOutboxFile()
        );
    }

    private User registerUserInternal(
            String username,
            String email,
            String password,
            String confirmPassword,
            UserRole role,
            boolean mustChangePassword
    ) {
        String normalizedUsername = Validator.requiredText(username, 80, "Username");
        String normalizedEmail = Validator.requiredEmail(email, "Email");
        Validator.password(password, confirmPassword);

        if (role == null) {
            throw new IllegalArgumentException("User role is required.");
        }

        if (USER_REPOSITORY.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("A user with this email already exists.");
        }

        String passwordHash = PasswordHasher.hash(password);
        User user = USER_REPOSITORY.insert(normalizedUsername, normalizedEmail, passwordHash, role, mustChangePassword);
        USERS.add(user);

        if (!mustChangePassword) {
            AuditService.recordForUser(user, "SELF_REGISTER", "USER", user.getEmail(), "SUCCESS", "Created cashier account from sign-up form.");
        }

        return user;
    }

    private void updateCachedPasswordHash(String email, String passwordHash) {
        updateCachedPassword(email, passwordHash, null);
    }

    private void updateCachedPassword(String email, String passwordHash, Boolean mustChangePassword) {
        for (User user : USERS) {
            if (user.getEmail().equals(email)) {
                user.setPasswordHash(passwordHash);

                if (mustChangePassword != null) {
                    user.setMustChangePassword(mustChangePassword);
                }

                return;
            }
        }
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(TEMP_PASSWORD_LENGTH);

        for (int index = 0; index < TEMP_PASSWORD_LENGTH; index++) {
            int randomIndex = SECURE_RANDOM.nextInt(TEMP_PASSWORD_ALPHABET.length());
            password.append(TEMP_PASSWORD_ALPHABET.charAt(randomIndex));
        }

        return password.toString();
    }

    private String generateResetCode() {
        StringBuilder code = new StringBuilder(RESET_CODE_LENGTH);

        for (int index = 0; index < RESET_CODE_LENGTH; index++) {
            int randomIndex = SECURE_RANDOM.nextInt(RESET_CODE_ALPHABET.length());
            code.append(RESET_CODE_ALPHABET.charAt(randomIndex));
        }

        return code.toString();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public record UserCreationResult(User user, String temporaryPassword) {
    }


    public record PasswordResetRequestResult(String email, Instant expiresAt, boolean sentBySmtp, Path developmentOutboxFile) {
    }
}
