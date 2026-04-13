package test;

import main.EmailService;
import org.junit.jupiter.api.*;

import java.io.*;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CCAC tests for EmailService's two public methods.
 *
 * Both methods share the same predicate structure:
 *   P1 = c1 || c2   where  c1 = SMTP_USER.isEmpty(),  c2 = SMTP_PASS.isEmpty()
 *
 * T1 cases require SMTP credentials to be absent (empty strings in config).
 * T2 cases require real credentials in config.properties.
 * The tests detect the current config state at runtime and skip
 * whichever set of cases is infeasible.
 */
class EmailServiceCCACTest {

    // ── Detect javax.mail availability and SMTP configuration ─────────────
    private static boolean smtpConfigured;

    @BeforeAll
    static void detectEnvironment() {
        // Skip entire class if javax.mail / javax.activation are missing
        try {
            Class.forName("javax.mail.Session");
            Class.forName("javax.activation.DataHandler");
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            Assumptions.assumeTrue(false,
                    "javax.mail or javax.activation not on test classpath — skipping all EmailService CCAC tests");
        }

        // Detect whether SMTP credentials are present by reading the private static field
        try {
            Field f = EmailService.class.getDeclaredField("SMTP_USER");
            f.setAccessible(true);
            String user = (String) f.get(null);
            smtpConfigured = user != null && !user.isEmpty();
        } catch (Exception e) {
            smtpConfigured = false;
        }
    }

    private ByteArrayOutputStream outCapture;
    private PrintStream originalOut;

    @BeforeEach
    void captureStdout() {
        outCapture  = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outCapture));
    }

    @AfterEach
    void restoreStdout() {
        System.setOut(originalOut);
    }

    private String captured() {
        try { return outCapture.toString("UTF-8"); }
        catch (Exception e) { return outCapture.toString(); }
    }

    // ══════════════════════════════════════════════════════════════════════
    // sendBookingConfirmation
    // P1 = c1 || c2   c1 = SMTP_USER.isEmpty()   c2 = SMTP_PASS.isEmpty()
    // ══════════════════════════════════════════════════════════════════════

    // ── Active clause = c1 ────────────────────────────────────────────────

    /**
     * T1 – c1=T (SMTP_USER empty) → P1=T → early-return, logs to stdout.
     * Requires: no smtp.user in config.properties.
     */
    @Test
    void sendBookingConfirmation_c1_T1_smtpUserEmpty_logsToStdout() {
        Assumptions.assumeFalse(smtpConfigured,
                "SMTP is configured — T1 (SMTP_USER empty) is infeasible; run without credentials");
        EmailService.sendBookingConfirmation(
                "book@example.com", "Montreal", "Music", "2026-01-01");
        String out = captured();
        assertTrue(out.contains("EMAIL (SMTP not configured)"),
                "Expected early-return log, got: " + out);
        assertTrue(out.contains("book@example.com"));
    }

    /**
     * T2 – c1=F (SMTP_USER non-empty), c2=F → P1=F → SMTP send attempted.
     * Requires: smtp.user and smtp.password in config.properties.
     */
    @Test
    void sendBookingConfirmation_c1_T2_smtpUserConfigured_attemptsSend() {
        Assumptions.assumeTrue(smtpConfigured,
                "SMTP not configured — T2 requires credentials in config.properties");
        EmailService.sendBookingConfirmation(
                "book@example.com", "Montreal", "Music", "2026-01-01");
        assertFalse(captured().contains("EMAIL (SMTP not configured)"));
    }

    // ── Active clause = c2 ────────────────────────────────────────────────

    /**
     * T1 – c2=T (SMTP_PASS empty) → P1=T → early-return, logs to stdout.
     */
    @Test
    void sendBookingConfirmation_c2_T1_smtpPassEmpty_logsToStdout() {
        Assumptions.assumeFalse(smtpConfigured,
                "SMTP is configured — T1 (SMTP_PASS empty) is infeasible; run without credentials");
        EmailService.sendBookingConfirmation(
                "book2@example.com", "Toronto", "Concert", "2026-06-01");
        assertTrue(captured().contains("EMAIL (SMTP not configured)"));
    }

    /**
     * T2 – c2=F (SMTP_PASS non-empty), c1=F → P1=F → SMTP send attempted.
     */
    @Test
    void sendBookingConfirmation_c2_T2_smtpPassConfigured_attemptsSend() {
        Assumptions.assumeTrue(smtpConfigured,
                "SMTP not configured — T2 requires credentials in config.properties");
        EmailService.sendBookingConfirmation(
                "book2@example.com", "Toronto", "Concert", "2026-06-01");
        assertFalse(captured().contains("EMAIL (SMTP not configured)"));
    }

    // ══════════════════════════════════════════════════════════════════════
    // sendCancelationConfirmation
    // P1 = c3 || c4   c3 = SMTP_USER.isEmpty()   c4 = SMTP_PASS.isEmpty()
    // ══════════════════════════════════════════════════════════════════════

    // ── Active clause = c3 ────────────────────────────────────────────────

    /**
     * T1 – c3=T (SMTP_USER empty) → P1=T → early-return, logs to stdout.
     */
    @Test
    void sendCancelationConfirmation_c3_T1_smtpUserEmpty_logsToStdout() {
        Assumptions.assumeFalse(smtpConfigured,
                "SMTP is configured — T1 (SMTP_USER empty) is infeasible; run without credentials");
        EmailService.sendCancellationConfirmation(
                "cancel@example.com", "Montreal", "Music", "2026-01-01");
        String out = captured();
        assertTrue(out.contains("EMAIL (SMTP not configured)"),
                "Expected early-return log, got: " + out);
        assertTrue(out.contains("cancel@example.com"));
    }

    /**
     * T2 – c3=F, c4=F → P1=F → SMTP send attempted.
     */
    @Test
    void sendCancelationConfirmation_c3_T2_smtpUserConfigured_attemptsSend() {
        Assumptions.assumeTrue(smtpConfigured,
                "SMTP not configured — T2 requires credentials in config.properties");
        EmailService.sendCancellationConfirmation(
                "cancel@example.com", "Montreal", "Music", "2026-01-01");
        assertFalse(captured().contains("EMAIL (SMTP not configured)"));
    }

    // ── Active clause = c4 ────────────────────────────────────────────────

    /**
     * T1 – c4=T (SMTP_PASS empty) → P1=T → early-return, logs to stdout.
     */
    @Test
    void sendCancelationConfirmation_c4_T1_smtpPassEmpty_logsToStdout() {
        Assumptions.assumeFalse(smtpConfigured,
                "SMTP is configured — T1 (SMTP_PASS empty) is infeasible; run without credentials");
        EmailService.sendCancellationConfirmation(
                "cancel2@test.com", "Ottawa", "Sports", "2026-02-01");
        assertTrue(captured().contains("EMAIL (SMTP not configured)"));
    }

    /**
     * T2 – c4=F, c3=F → P1=F → SMTP send attempted.
     */
    @Test
    void sendCancelationConfirmation_c4_T2_smtpPassConfigured_attemptsSend() {
        Assumptions.assumeTrue(smtpConfigured,
                "SMTP not configured — T2 requires credentials in config.properties");
        EmailService.sendCancellationConfirmation(
                "cancel2@test.com", "Ottawa", "Sports", "2026-02-01");
        assertFalse(captured().contains("EMAIL (SMTP not configured)"));
    }
}
