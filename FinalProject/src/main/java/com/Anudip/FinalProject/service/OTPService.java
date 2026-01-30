package com.Anudip.FinalProject.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.twilio.Twilio;
import com.twilio.rest.verify.v2.service.Verification;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OTPService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final SecureRandom random = new SecureRandom();

    // Store OTPs with expiry time (in production, use Redis or database)
    private final Map<String, OTPData> otpStorage = new ConcurrentHashMap<>();

    @Autowired
    private EmailService emailService;

    // Twilio SMS configuration
    @Value("${twilio.sms.account-sid:}")
    private String twilioAccountSid;

    @Value("${twilio.sms.auth-token:}")
    private String twilioAuthToken;

    @Value("${twilio.sms.verify-service-sid:}")
    private String verifyServiceSid;

    @Value("${otp.test.mode:true}")
    private boolean testMode;

    private static class OTPData {
        final String otp;
        final LocalDateTime expiry;
        int attempts;

        OTPData(String otp, LocalDateTime expiry) {
            this.otp = otp;
            this.expiry = expiry;
            this.attempts = 0;
        }
    }

    /**
     * Initialize Twilio with credentials if provided
     */
    private void initializeTwilio() {
        if (twilioAccountSid != null && !twilioAccountSid.isEmpty() && 
            twilioAuthToken != null && !twilioAuthToken.isEmpty()) {
            try {
                Twilio.init(twilioAccountSid, twilioAuthToken);
                System.out.println("Twilio SMS initialized successfully");
            } catch (Exception e) {
                System.err.println("Failed to initialize Twilio SMS: " + e.getMessage());
            }
        } else {
            System.out.println("Twilio SMS credentials not configured - using test mode");
        }
    }

    /**
     * Generate a random numeric OTP
     */
    public String generateOTP() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    /**
     * Send OTP to email
     */
    public void sendOTPToEmail(String email, String otp) {
        // Store OTP with email as key
        storeOTP(email, otp);
        
        if (testMode) {
            // In test mode, show OTP clearly in console
            System.out.println("\n=========================================================");
            System.out.println("                    EMAIL OTP (TEST MODE)                  ");
            System.out.println("=========================================================");
            System.out.println("TO:     " + email);
            System.out.println("OTP:    " + otp);
            System.out.println("EXPIRY: " + OTP_EXPIRY_MINUTES + " minutes");
            System.out.println("=========================================================");
            System.out.println(">>> COPY THIS OTP AND ENTER IT IN THE VERIFICATION FIELD <<<");
            System.out.println("=========================================================\n");
        } else {
            try {
                // Try to send real email
                emailService.sendOTPEmail(email, otp);
                System.out.println("Email OTP sent to: " + email);
            } catch (Exception e) {
                System.err.println("Email service failed: " + e.getMessage());
                System.out.println("Fallback - OTP for " + email + ": " + otp);
            }
        }
    }

    /**
     * Send OTP to phone using Twilio Verify API
     */
    public void sendOTPToPhone(String phone, String otp) {
        // Store OTP with phone as key
        storeOTP(phone, otp);
        
        if (testMode) {
            // In test mode, just log to console
            System.out.println("\n=========================================================");
            System.out.println("                    SMS OTP (TEST MODE)                    ");
            System.out.println("=========================================================");
            System.out.println("TO:     " + phone);
            System.out.println("OTP:    " + otp);
            System.out.println("EXPIRY: " + OTP_EXPIRY_MINUTES + " minutes");
            System.out.println("=========================================================");
            System.out.println(">>> COPY THIS OTP AND ENTER IT IN THE VERIFICATION FIELD <<<");
            System.out.println("=========================================================\n");
        } else {
            // Send real SMS via Twilio Verify API
            sendTwilioVerifySMS(phone, otp);
        }
    }

    /**
     * Send SMS using Twilio Verify API
     */
    private void sendTwilioVerifySMS(String phoneNumber, String otp) {
        initializeTwilio();
        
        if (twilioAccountSid == null || twilioAccountSid.isEmpty() || 
            twilioAuthToken == null || twilioAuthToken.isEmpty() ||
            verifyServiceSid == null || verifyServiceSid.isEmpty()) {
            System.out.println("\n=========================================================");
            System.out.println("               TWILIO NOT CONFIGURED                       ");
            System.out.println("=========================================================");
            System.out.println("Phone: " + phoneNumber);
            System.out.println("OTP:   " + otp);
            System.out.println("=========================================================\n");
            return;
        }

        try {
            // Format phone number for Twilio
            String formattedPhone = formatPhoneNumber(phoneNumber);
            
            // Create verification using Twilio Verify API
            Verification verification = Verification.creator(
                    verifyServiceSid,
                    formattedPhone,
                    "sms")
                .create();

            System.out.println("\n=========================================================");
            System.out.println("              TWILIO VERIFY SMS SENT!                      ");
            System.out.println("=========================================================");
            System.out.println("To:     " + phoneNumber);
            System.out.println("SID:    " + verification.getSid());
            System.out.println("Status: " + verification.getStatus());
            System.out.println("=========================================================\n");
            
        } catch (Exception e) {
            System.err.println("TWILIO SMS FAILED: " + e.getMessage());
            System.out.println("Fallback - OTP for " + phoneNumber + ": " + otp);
        }
    }

    /**
     * Format phone number to international format
     */
    private String formatPhoneNumber(String phone) {
        if (phone == null) {
            return null;
        }
        String cleaned = phone.replaceAll("[^0-9]", "");
        if (cleaned.length() == 10) {
            return "+91" + cleaned;
        } else if (!cleaned.startsWith("91") && cleaned.length() == 11) {
            return "+" + cleaned;
        } else if (!cleaned.startsWith("+")) {
            return "+" + cleaned;
        }
        return phone;
    }

    /**
     * Store OTP with expiry time
     */
    private void storeOTP(String key, String otp) {
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
        otpStorage.put(key, new OTPData(otp, expiry));
        System.out.println("OTP stored for: " + key);
    }

    /**
     * Verify OTP (local verification)
     */
    public OTPVerificationResult verifyOTP(String key, String enteredOTP) {
        if (key == null || enteredOTP == null) {
            return new OTPVerificationResult(false, "Invalid input parameters");
        }

        OTPData otpData = otpStorage.get(key);

        if (otpData == null) {
            return new OTPVerificationResult(false, "OTP not found or expired. Please request a new OTP.");
        }

        // Check if expired
        if (LocalDateTime.now().isAfter(otpData.expiry)) {
            otpStorage.remove(key);
            return new OTPVerificationResult(false, "OTP has expired. Please request a new OTP.");
        }

        // Check attempts
        if (otpData.attempts >= 3) {
            otpStorage.remove(key);
            return new OTPVerificationResult(false, "Too many failed attempts. Please request a new OTP.");
        }

        // Increment attempts
        otpData.attempts++;

        // Verify OTP
        if (otpData.otp.equals(enteredOTP)) {
            otpStorage.remove(key);
            System.out.println("OTP verified successfully for: " + key);
            return new OTPVerificationResult(true, "OTP verified successfully!");
        }

        int attemptsLeft = 3 - otpData.attempts;
        return new OTPVerificationResult(false, "Invalid OTP. " + attemptsLeft + " attempts remaining.");
    }

    /**
     * Check if OTP was sent to this key
     */
    public boolean hasPendingOTP(String key) {
        if (key == null) {
            return false;
        }
        OTPData otpData = otpStorage.get(key);
        if (otpData == null) {
            return false;
        }
        // Check if expired
        if (LocalDateTime.now().isAfter(otpData.expiry)) {
            otpStorage.remove(key);
            return false;
        }
        return true;
    }

    /**
     * Clear OTP (for logout/cancel)
     */
    public void clearOTP(String key) {
        if (key != null) {
            otpStorage.remove(key);
        }
    }

    /**
     * Clear all expired OTPs (call periodically)
     */
    public void cleanupExpiredOTPs() {
        otpStorage.entrySet().removeIf(entry -> 
            LocalDateTime.now().isAfter(entry.getValue().expiry));
    }

    /**
     * Result class for OTP verification
     */
    public static class OTPVerificationResult {
        private final boolean success;
        private final String message;

        public OTPVerificationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { 
            return success; 
        }
        
        public String getMessage() { 
            return message; 
        }
    }
}

