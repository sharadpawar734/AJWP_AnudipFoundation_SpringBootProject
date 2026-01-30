package com.Anudip.FinalProject.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.twilio.Twilio;
import com.twilio.rest.verify.v2.service.Verification;

/**
 * Service for sending OTP via Email using Twilio SendGrid
 */
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${otp.email.from:noreply@bookstore.com}")
    private String fromEmail;

    @Value("${otp.email.subject:Your BookStore OTP Code}")
    private String emailSubject;

    @Value("${otp.test.mode:true}")
    private boolean testMode;

    // Twilio Email (SendGrid) configuration
    @Value("${twilio.email.account-sid:}")
    private String twilioEmailAccountSid;

    @Value("${twilio.email.auth-token:}")
    private String twilioEmailAuthToken;

    @Value("${twilio.email.verify-service-sid:}")
    private String twilioEmailVerifyServiceSid;

    /**
     * Initialize Twilio Email with credentials if provided
     */
    private void initializeTwilioEmail() {
        if (twilioEmailAccountSid != null && !twilioEmailAccountSid.isEmpty() && 
            twilioEmailAuthToken != null && !twilioEmailAuthToken.isEmpty()) {
            try {
                Twilio.init(twilioEmailAccountSid, twilioEmailAuthToken);
                System.out.println("Twilio Email (SendGrid) initialized successfully");
            } catch (Exception e) {
                System.err.println("Failed to initialize Twilio Email: " + e.getMessage());
            }
        } else {
            System.out.println("Twilio Email credentials not configured");
        }
    }

    /**
     * Send OTP email to the specified address
     * 
     * @param toEmail recipient email address
     * @param otp      the OTP code to send
     */
    public void sendOTPEmail(String toEmail, String otp) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            System.err.println("Cannot send OTP: email is null or empty");
            return;
        }

        if (testMode) {
            // In test mode, just log to console
            System.out.println("\n=========================================================");
            System.out.println("                    EMAIL OTP (TEST MODE)                  ");
            System.out.println("=========================================================");
            System.out.println("TO:     " + toEmail);
            System.out.println("OTP:    " + otp);
            System.out.println("EXPIRY: 5 minutes");
            System.out.println("=========================================================\n");
            return;
        }

        // Try Twilio Verify API for email first
        if (sendViaTwilioVerifyEmail(toEmail, otp)) {
            return; // Success via Twilio
        }

        // Fallback to regular email if Twilio fails
        sendViaJavaMail(toEmail, otp);
    }

    /**
     * Send OTP via Twilio Verify API (email channel)
     * Note: Twilio Verify for email requires special setup
     */
    private boolean sendViaTwilioVerifyEmail(String email, String otp) {
        initializeTwilioEmail();
        
        if (twilioEmailAccountSid == null || twilioEmailAccountSid.isEmpty() || 
            twilioEmailAuthToken == null || twilioEmailAuthToken.isEmpty() ||
            twilioEmailVerifyServiceSid == null || twilioEmailVerifyServiceSid.isEmpty()) {
            System.out.println("Twilio Email Verify not configured, using fallback");
            return false;
        }

        try {
            // Create verification using Twilio Verify API for email
            Verification verification = Verification.creator(
                    twilioEmailVerifyServiceSid,
                    email,
                    "email")
                .create();

            System.out.println("\n=========================================================");
            System.out.println("              TWILIO VERIFY EMAIL SENT!                    ");
            System.out.println("=========================================================");
            System.out.println("To:     " + email);
            System.out.println("SID:    " + verification.getSid());
            System.out.println("Status: " + verification.getStatus());
            System.out.println("=========================================================\n");
            return true;
            
        } catch (Exception e) {
            System.err.println("TWILIO EMAIL VERIFY FAILED: " + e.getMessage());
            return false;
        }
    }

    /**
     * Send OTP via regular email (fallback using JavaMail)
     */
    private void sendViaJavaMail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(emailSubject);
            message.setText(buildEmailContent(otp));
            
            mailSender.send(message);
            System.out.println("\n=========================================================");
            System.out.println("              EMAIL SENT VIA JAVA MAIL (SENDGRID)          ");
            System.out.println("=========================================================");
            System.out.println("To:     " + toEmail);
            System.out.println("=========================================================\n");
        } catch (Exception e) {
            System.err.println("FAILED TO SEND EMAIL TO: " + toEmail);
            System.err.println("Error: " + e.getMessage());
            
            // Final fallback - log to console
            System.out.println("\n=========================================================");
            System.out.println("                    EMAIL OTP (FALLBACK)                   ");
            System.out.println("=========================================================");
            System.out.println("TO:     " + toEmail);
            System.out.println("OTP:    " + otp);
            System.out.println("=========================================================\n");
        }
    }

    /**
     * Build the email content for OTP
     */
    private String buildEmailContent(String otp) {
        StringBuilder content = new StringBuilder();
        content.append("=========================================================\n");
        content.append("        BookStore OTP Verification       \n");
        content.append("=========================================================\n\n");
        content.append("Dear Customer,\n\n");
        content.append("Your One-Time Password (OTP) for account verification is:\n\n");
        content.append("        ** ").append(otp).append(" **\n\n");
        content.append("This OTP is valid for 5 minutes and can only be used once.\n\n");
        content.append("If you did not request this OTP, please ignore this email.\n\n");
        content.append("For security reasons, please do not share this OTP with anyone.\n\n");
        content.append("Best regards,\n");
        content.append("BookStore Team\n");
        content.append("=========================================================");
        return content.toString();
    }

    /**
     * Send welcome email after successful OTP verification
     */
    public void sendWelcomeEmail(String toEmail, String username) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            System.err.println("Cannot send welcome email: email is null or empty");
            return;
        }

        if (testMode) {
            System.out.println("\n=========================================================");
            System.out.println("                  WELCOME EMAIL (TEST MODE)                ");
            System.out.println("=========================================================");
            System.out.println("To:      " + toEmail);
            System.out.println("Username: " + username);
            System.out.println("=========================================================\n");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Welcome to BookStore - Account Verified!");
            message.setText(buildWelcomeEmailContent(username));
            
            mailSender.send(message);
            System.out.println("Welcome email sent to: " + toEmail);
        } catch (Exception e) {
            System.err.println("FAILED TO SEND WELCOME EMAIL TO: " + toEmail);
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * Build welcome email content
     */
    private String buildWelcomeEmailContent(String username) {
        StringBuilder content = new StringBuilder();
        content.append("=========================================================\n");
        content.append("     Welcome to BookStore Family!       \n");
        content.append("=========================================================\n\n");
        content.append("Dear ").append(username).append(",\n\n");
        content.append("Congratulations! Your BookStore account has been successfully verified.\n\n");
        content.append("You now have access to:\n");
        content.append("  - Browse thousands of books\n");
        content.append("  - Add items to wishlist and cart\n");
        content.append("  - Easy checkout process\n");
        content.append("  - Order tracking\n");
        content.append("  - Exclusive member offers\n\n");
        content.append("Start exploring your favorite books today!\n\n");
        content.append("Happy Reading,\n");
        content.append("BookStore Team\n");
        content.append("=========================================================");
        return content.toString();
    }
}

