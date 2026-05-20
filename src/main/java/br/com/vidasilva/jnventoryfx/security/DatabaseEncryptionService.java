package br.com.vidasilva.jnventoryfx.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

public final class DatabaseEncryptionService {
    private static final String ENVIRONMENT_KEY = "JNVENTORYFX_DB_KEY";
    private static final Path DEV_KEY_FILE = Path.of(".jnventoryfx-dev-key");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private DatabaseEncryptionService() {
    }

    public static String loadSqlCipherKey() throws IOException {
        String configuredKey = System.getenv(ENVIRONMENT_KEY);

        if (configuredKey != null && !configuredKey.isBlank()) {
            return configuredKey.trim();
        }

        if (!Files.exists(DEV_KEY_FILE)) {
            byte[] keyBytes = new byte[32];
            SECURE_RANDOM.nextBytes(keyBytes);
            Files.writeString(DEV_KEY_FILE, Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes), StandardCharsets.UTF_8);
        }

        return Files.readString(DEV_KEY_FILE, StandardCharsets.UTF_8).trim();
    }

    public static String toSqlLiteral(String value) {
        if (value == null) {
            return "''";
        }

        return "'" + value.replace("'", "''") + "'";
    }
}
