package br.com.vidasilva.jnventoryfx.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import javax.net.ssl.SSLSocketFactory;

public class EmailService {
    private static final String SMTP_HOST = "JNVENTORYFX_SMTP_HOST";
    private static final String SMTP_PORT = "JNVENTORYFX_SMTP_PORT";
    private static final String SMTP_USERNAME = "JNVENTORYFX_SMTP_USERNAME";
    private static final String SMTP_PASSWORD = "JNVENTORYFX_SMTP_PASSWORD";
    private static final String SMTP_FROM = "JNVENTORYFX_SMTP_FROM";
    private static final String SMTP_SSL = "JNVENTORYFX_SMTP_SSL";
    private static final String SMTP_STARTTLS = "JNVENTORYFX_SMTP_STARTTLS";
    private static final Path DEV_OUTBOX_DIRECTORY = Path.of("dev-mail-outbox");
    private static final DateTimeFormatter HUMAN_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm z")
            .withZone(ZoneId.systemDefault());

    public PasswordResetMailResult sendPasswordResetCode(String toEmail, String resetCode, Instant expiresAt) {
        SmtpConfig config = SmtpConfig.fromEnvironment();
        String subject = "JnventoryFX password reset code";
        String body = """
                A password reset was requested for your JnventoryFX account.

                Reset code: %s
                Expires at: %s

                Enter this code in the JnventoryFX password recovery dialog and choose a new password.
                If you did not request this, ignore this email.
                """.formatted(resetCode, HUMAN_TIME_FORMATTER.format(expiresAt));

        if (config.isConfigured()) {
            sendViaSmtp(config, toEmail, subject, body);
            return new PasswordResetMailResult(true, null);
        }

        Path outboxFile = writeDevelopmentOutboxEmail(toEmail, subject, body);
        return new PasswordResetMailResult(false, outboxFile);
    }

    private void sendViaSmtp(SmtpConfig config, String toEmail, String subject, String body) {
        try (SmtpSession session = SmtpSession.open(config)) {
            session.expectGreeting();
            session.ehlo();

            if (config.startTls()) {
                session.startTls();
                session.ehlo();
            }

            if (config.hasCredentials()) {
                session.authenticate();
            }

            session.sendMail(config.from(), toEmail, buildMessage(config.from(), toEmail, subject, body));
            session.quit();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not send password reset email through SMTP.", exception);
        }
    }

    private Path writeDevelopmentOutboxEmail(String toEmail, String subject, String body) {
        try {
            Files.createDirectories(DEV_OUTBOX_DIRECTORY);
            String safeEmail = toEmail.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path outboxFile = DEV_OUTBOX_DIRECTORY.resolve("password-reset-" + safeEmail + "-" + System.currentTimeMillis() + ".txt");
            Files.writeString(outboxFile, buildMessage("dev-outbox@jnventoryfx.local", toEmail, subject, body), StandardCharsets.UTF_8);
            return outboxFile;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not write development password reset email.", exception);
        }
    }

    private String buildMessage(String from, String to, String subject, String body) {
        return "From: " + from + "\r\n"
                + "To: " + to + "\r\n"
                + "Subject: " + subject + "\r\n"
                + "MIME-Version: 1.0\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\n"
                + "Content-Transfer-Encoding: 8bit\r\n"
                + "\r\n"
                + body.replace("\n", "\r\n");
    }

    public record PasswordResetMailResult(boolean sentBySmtp, Path developmentOutboxFile) {
    }

    private record SmtpConfig(
            String host,
            int port,
            String username,
            String password,
            String from,
            boolean ssl,
            boolean startTls
    ) {
        static SmtpConfig fromEnvironment() {
            String host = getenv(SMTP_HOST);
            String username = getenv(SMTP_USERNAME);
            String password = getenv(SMTP_PASSWORD);
            String from = getenv(SMTP_FROM);
            boolean ssl = Boolean.parseBoolean(getenvOrDefault(SMTP_SSL, "false"));
            boolean startTls = Boolean.parseBoolean(getenvOrDefault(SMTP_STARTTLS, ssl ? "false" : "true"));
            int defaultPort = ssl ? 465 : 587;
            int port = parsePort(getenv(SMTP_PORT), defaultPort);

            if (from == null || from.isBlank()) {
                from = username == null || username.isBlank() ? "no-reply@jnventoryfx.local" : username;
            }

            return new SmtpConfig(host, port, username, password, from, ssl, startTls);
        }

        boolean isConfigured() {
            return host != null && !host.isBlank();
        }

        boolean hasCredentials() {
            return username != null && !username.isBlank() && password != null && !password.isBlank();
        }

        private static int parsePort(String value, int fallback) {
            if (value == null || value.isBlank()) {
                return fallback;
            }

            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException exception) {
                return fallback;
            }
        }

        private static String getenv(String name) {
            String value = System.getenv(name);
            return value == null ? null : value.trim();
        }

        private static String getenvOrDefault(String name, String fallback) {
            String value = getenv(name);
            return value == null || value.isBlank() ? fallback : value;
        }
    }

    private static final class SmtpSession implements AutoCloseable {
        private final SmtpConfig config;
        private Socket socket;
        private BufferedReader reader;
        private BufferedWriter writer;

        private SmtpSession(SmtpConfig config, Socket socket) throws IOException {
            this.config = config;
            this.socket = socket;
            configureStreams();
        }

        static SmtpSession open(SmtpConfig config) throws IOException {
            Socket socket = config.ssl()
                    ? SSLSocketFactory.getDefault().createSocket(config.host(), config.port())
                    : new Socket(config.host(), config.port());
            return new SmtpSession(config, socket);
        }

        void expectGreeting() throws IOException {
            expect(220);
        }

        void ehlo() throws IOException {
            command("EHLO localhost", 250);
        }

        void startTls() throws IOException {
            command("STARTTLS", 220);
            socket = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(socket, config.host(), config.port(), true);
            configureStreams();
        }

        void authenticate() throws IOException {
            command("AUTH LOGIN", 334);
            command(Base64.getEncoder().encodeToString(config.username().getBytes(StandardCharsets.UTF_8)), 334);
            command(Base64.getEncoder().encodeToString(config.password().getBytes(StandardCharsets.UTF_8)), 235);
        }

        void sendMail(String from, String to, String message) throws IOException {
            command("MAIL FROM:<" + from + ">", 250);
            command("RCPT TO:<" + to + ">", 250, 251);
            command("DATA", 354);

            for (String line : message.split("\r?\n", -1)) {
                if (line.startsWith(".")) {
                    line = "." + line;
                }
                writeLine(line);
            }

            writeLine(".");
            expect(250);
        }

        void quit() throws IOException {
            command("QUIT", 221);
        }

        private void command(String command, int... expectedCodes) throws IOException {
            writeLine(command);
            int actualCode = readReplyCode();

            for (int expectedCode : expectedCodes) {
                if (actualCode == expectedCode) {
                    return;
                }
            }

            throw new IOException("SMTP server returned " + actualCode + " for command: " + command);
        }

        private void expect(int... expectedCodes) throws IOException {
            int actualCode = readReplyCode();

            for (int expectedCode : expectedCodes) {
                if (actualCode == expectedCode) {
                    return;
                }
            }

            throw new IOException("SMTP server returned unexpected code: " + actualCode);
        }

        private int readReplyCode() throws IOException {
            String line = reader.readLine();

            if (line == null || line.length() < 3) {
                throw new IOException("SMTP server closed the connection or sent an invalid reply.");
            }

            int code = Integer.parseInt(line.substring(0, 3));

            while (line.length() > 3 && line.charAt(3) == '-') {
                line = reader.readLine();

                if (line == null || line.length() < 3) {
                    throw new IOException("SMTP server closed the connection during a multiline reply.");
                }
            }

            return code;
        }

        private void writeLine(String line) throws IOException {
            writer.write(line);
            writer.write("\r\n");
            writer.flush();
        }

        private void configureStreams() throws IOException {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public void close() throws IOException {
            if (socket != null) {
                socket.close();
            }
        }
    }
}
