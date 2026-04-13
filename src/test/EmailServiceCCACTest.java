package test;

import main.EmailService;
import org.junit.jupiter.api.*;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CCAC tests for EmailService's two public methods.
 *
 * Both methods share the same predicate structure:
 *   P1 = c1 || c2   where  c1 = SMTP_USER.isEmpty(),  c2 = SMTP_PASS.isEmpty()
 *
 * With the default config.properties (no smtp.user / smtp.password entries),
 * SMTP_USER = "" and SMTP_PASS = "", so:
 *   - T1 cases (at least one empty → early-return path) are directly exercisable:
 *     the method logs "EMAIL (SMTP not configured) …" to stdout.
 *   - T2 cases (both non-empty → SMTP send attempted) require real SMTP
 *     credentials.  Those tests are marked @Disabled and serve as
 *     documentation of the expected behaviour when credentials are present.
 *
 * sendCancelationConfirmation uses the same predicate; the table labels its
 * SMTP_USER clause as c3 and SMTP_PASS clause as c4 to distinguish them.
 */
class EmailServiceCCACTest {

    // ── Skip entire class if javax.mail is not on the runtime classpath ────
    // Even the early-return (SMTP_USER empty) path requires the JVM to verify
    // the full method bytecode, which references javax.mail.Authenticator.
    @BeforeAll
    static void requireJavaxMail() {
        try {
            Class.forName("javax.mail.Session");
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            Assumptions.assumeTrue(false,
                    "javax.mail not on test classpath — skipping all EmailService CCAC tests");
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
     * T1 – c1=T (SMTP_USER empty), c2=F not required to be distinct:
     * with default config both fields are empty, so P1 is true and
     * the method takes the early-return logging path.
     */
    @Test
    void sendBookingConfirmation_c1_T1_smtpUserEmpty_logsToStdout() {
        EmailService.sendBookingConfirmation(
                "book@example.com", "Montreal", "Music", "2026-01-01");
        String out = captured();
        assertTrue(out.contains("EMAIL (SMTP not configured)"),
                "Expected early-return log, got: " + out);
        assertTrue(out.contains("book@example.com"));
    }

    /**
     * T2 – c1=F (SMTP_USER non-empty), c2=F (SMTP_PASS non-empty):
     * P1=F → method attempts actual SMTP send.
     * Disabled: requires smtp.user and smtp.password in config.properties.
     */
    @Test
    @Disabled("Requires SMTP credentials: set smtp.user and smtp.password in config.properties")
    void sendBookingConfirmation_c1_T2_smtpUserConfigured_attemptsSend() {
        EmailService.sendBookingConfirmation(
                "book@example.com", "Montreal", "Music", "2026-01-01");
        assertFalse(captured().contains("EMAIL (SMTP not configured)"));
    }

    // ── Active clause = c2 ────────────────────────────────────────────────

    /**
     * T1 – c2=T (SMTP_PASS empty), c1=F not required to be distinct:
     * P1=T → early-return logging path.
     */
    @Test
    void sendBookingConfirmation_c2_T1_smtpPassEmpty_logsToStdout() {
        EmailService.sendBookingConfirmation(
                "book2@example.com", "Toronto", "Concert", "2026-06-01");
        assertTrue(captured().contains("EMAIL (SMTP not configured)"));
    }

    /**
     * T2 – c2=F (SMTP_PASS non-empty) and c1=F (SMTP_USER non-empty):
     * P1=F → SMTP send attempted.
     * Disabled: requires SMTP credentials.
     */
    @Test
    @Disabled("Requires SMTP credentials: set smtp.user and smtp.password in config.properties")
    void sendBookingConfirmation_c2_T2_smtpPassConfigured_attemptsSend() {
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
     * T1 – c3=T (SMTP_USER empty): P1=T → early-return logging path.
     */
    @Test
    void sendCancelationConfirmation_c3_T1_smtpUserEmpty_logsToStdout() {
        EmailService.sendCancellationConfirmation(
                "cancel@example.com", "Montreal", "Music", "2026-01-01");
        String out = captured();
        assertTrue(out.contains("EMAIL (SMTP not configured)"),
                "Expected early-return log, got: " + out);
        assertTrue(out.contains("cancel@example.com"));
    }

    /**
     * T2 – c3=F, c4=F: P1=F → SMTP send attempted.
     * Disabled: requires SMTP credentials.
     */
    @Test
    @Disabled("Requires SMTP credentials: set smtp.user and smtp.password in config.properties")
    void sendCancelationConfirmation_c3_T2_smtpUserConfigured_attemptsSend() {
        EmailService.sendCancellationConfirmation(
                "cancel@example.com", "Montreal", "Music", "2026-01-01");
        assertFalse(captured().contains("EMAIL (SMTP not configured)"));
    }

    // ── Active clause = c4 ────────────────────────────────────────────────

    /**
     * T1 – c4=T (SMTP_PASS empty): P1=T → early-return logging path.
     */
    @Test
    void sendCancelationConfirmation_c4_T1_smtpPassEmpty_logsToStdout() {
        EmailService.sendCancellationConfirmation(
                "cancel2@test.com", "Ottawa", "Sports", "2026-02-01");
        assertTrue(captured().contains("EMAIL (SMTP not configured)"));
    }

    /**
     * T2 – c4=F, c3=F: P1=F → SMTP send attempted.
     * Disabled: requires SMTP credentials.
     */
    @Test
    @Disabled("Requires SMTP credentials: set smtp.user and smtp.password in config.properties")
    void sendCancelationConfirmation_c4_T2_smtpPassConfigured_attemptsSend() {
        EmailService.sendCancellationConfirmation(
                "cancel2@test.com", "Ottawa", "Sports", "2026-02-01");
        assertFalse(captured().contains("EMAIL (SMTP not configured)"));
    }
}