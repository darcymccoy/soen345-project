package main;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class EmailService {

    private static final String SMTP_HOST;
    private static final String SMTP_PORT;
    private static final String SMTP_USER;
    private static final String SMTP_PASS;

    static {
        try {
            Properties config = new Properties();
            config.load(new java.io.FileInputStream("config.properties"));
            SMTP_HOST = config.getProperty("smtp.host", "smtp.gmail.com");
            SMTP_PORT = config.getProperty("smtp.port", "587");
            SMTP_USER = config.getProperty("smtp.user", "");
            SMTP_PASS = config.getProperty("smtp.password", "");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load email config from config.properties", e);
        }
    }

    public static void sendBookingConfirmation(String toEmail, String eventLocation, String eventCategory, String eventDate) {
        if (SMTP_USER.isEmpty() || SMTP_PASS.isEmpty()) {
            System.out.println("EMAIL (SMTP not configured) - Booking confirmation for " + toEmail);
            System.out.println("  Event: " + eventCategory + " at " + eventLocation + " on " + eventDate);
            System.out.println("  Status: Reservation confirmed");
            return;
        }

        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);

            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SMTP_USER, SMTP_PASS);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SMTP_USER));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Booking Confirmation - " + eventCategory);
            message.setText("Dear User,\n\n"
                + "Your reservation has been confirmed!\n\n"
                + "Event Details:\n"
                + "  Category: " + eventCategory + "\n"
                + "  Location: " + eventLocation + "\n"
                + "  Date: " + eventDate + "\n\n"
                + "Thank you for booking with Event Manager.\n\n"
                + "Best regards,\nEvent Manager Team");

            Transport.send(message);
            System.out.println("Booking confirmation email sent to " + toEmail);
        } catch (Exception e) {
            System.out.println("Failed to send email to " + toEmail + ": " + e.getMessage());
        }
    }

    public static void sendCancellationConfirmation(String toEmail, String eventLocation, String eventCategory, String eventDate) {
        if (SMTP_USER.isEmpty() || SMTP_PASS.isEmpty()) {
            System.out.println("EMAIL (SMTP not configured) - Cancellation confirmation for " + toEmail);
            System.out.println("  Event: " + eventCategory + " at " + eventLocation + " on " + eventDate);
            System.out.println("  Status: Reservation cancelled");
            return;
        }

        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);

            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SMTP_USER, SMTP_PASS);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SMTP_USER));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Reservation Cancelled - " + eventCategory);
            message.setText("Dear User,\n\n"
                + "Your reservation has been cancelled.\n\n"
                + "Event Details:\n"
                + "  Category: " + eventCategory + "\n"
                + "  Location: " + eventLocation + "\n"
                + "  Date: " + eventDate + "\n\n"
                + "If this was a mistake, you can rebook on Event Manager.\n\n"
                + "Best regards,\nEvent Manager Team");

            Transport.send(message);
            System.out.println("Cancellation email sent to " + toEmail);
        } catch (Exception e) {
            System.out.println("Failed to send email to " + toEmail + ": " + e.getMessage());
        }
    }
}
