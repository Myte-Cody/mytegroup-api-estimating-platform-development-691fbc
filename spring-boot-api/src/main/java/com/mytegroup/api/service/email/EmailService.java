package com.mytegroup.api.service.email;

import com.mytegroup.api.service.common.AuditLogService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for sending emails.
 * Handles email sending via SMTP with HTML support.
 * Mirrors NestJS email.service.ts implementation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;
    private final AuditLogService auditLogService;
    
    @Value("${spring.mail.from:no-reply@myte.test}")
    private String fromAddress;
    
    @Value("${app.client.base-url:http://localhost:4001}")
    private String clientBaseUrl;
    
    @Value("${app.email.stub-transport:false}")
    private boolean stubTransport;
    
    // Store sent messages for testing (only when stubTransport is true)
    private final List<Map<String, String>> sentMessages = new java.util.ArrayList<>();
    
    /**
     * Sends a plain text email.
     */
    public void sendEmail(String to, String subject, String text) {
        sendMail(to, subject, text, null, null);
    }
    
    /**
     * Sends an email with optional HTML content.
     */
    public void sendMail(String to, String subject, String text, String html, List<String> bcc) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("to", to);
        metadata.put("subject", subject);
        metadata.put("mode", stubTransport ? "stub" : "live");
        metadata.put("bccCount", bcc != null ? bcc.size() : 0);
        
        auditLogService.log(
            "email.send_attempt",
            null,
            null,
            null,
            null,
            metadata
        );
        
        try {
            doSend(to, subject, text, html, bcc);
            
            if (stubTransport) {
                sentMessages.add(Map.of("to", to, "subject", subject));
            }
            
            metadata.put("status", "sent");
            auditLogService.log(
                "email.send",
                null,
                null,
                null,
                null,
                metadata
            );
        } catch (Exception e) {
            // Retry once
            try {
                doSend(to, subject, text, html, bcc);
                
                if (stubTransport) {
                    sentMessages.add(Map.of("to", to, "subject", subject));
                }
                
                metadata.put("status", "sent-retry");
                auditLogService.log(
                    "email.send",
                    null,
                    null,
                    null,
                    null,
                    metadata
                );
            } catch (Exception e2) {
                metadata.put("status", "failed");
                metadata.put("error", e2.getMessage());
                auditLogService.log(
                    "email.send_failed",
                    null,
                    null,
                    null,
                    null,
                    metadata
                );
                throw new RuntimeException("Failed to send email", e2);
            }
        }
    }
    
    private void doSend(String to, String subject, String text, String html, List<String> bcc) {
        if (html != null && !html.isEmpty()) {
            // Send as MIME message with HTML
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                
                helper.setFrom(fromAddress);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(text != null ? text : "", html);
                
                if (bcc != null && !bcc.isEmpty()) {
                    helper.setBcc(bcc.toArray(new String[0]));
                }
                
                mailSender.send(message);
            } catch (MessagingException e) {
                throw new RuntimeException("Failed to create email message", e);
            }
        } else {
            // Send as simple text message
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text != null ? text : "");
            
            if (bcc != null && !bcc.isEmpty()) {
                message.setBcc(bcc.toArray(new String[0]));
            }
            
            mailSender.send(message);
        }
    }
    
    /**
     * Sends a branded email with consistent styling.
     */
    public void sendBrandedEmail(String email, String subject, String title, 
                                  String bodyHtml, String bodyText, 
                                  String ctaLabel, String ctaHref, String footerNote) {
        String html = renderBrandedTemplate(title, bodyHtml, ctaLabel, ctaHref, footerNote);
        sendMail(email, subject, bodyText, html, null);
    }
    
    /**
     * Sends a verification email.
     */
    public void sendVerificationEmail(String email, String token, String orgId, String userName) {
        String link = buildClientUrl("/verify-email?token=" + urlEncode(token));
        EmailTemplate template = verificationTemplate(link, userName);
        sendMail(email, template.subject(), template.text(), template.html(), null);
    }
    
    /**
     * Sends a password reset email.
     */
    public void sendPasswordResetEmail(String email, String token, String orgId, String userName) {
        String link = buildClientUrl("/reset-password?token=" + urlEncode(token));
        EmailTemplate template = passwordResetTemplate(link, userName);
        sendMail(email, template.subject(), template.text(), template.html(), null);
    }
    
    /**
     * Sends an invite email.
     */
    public void sendInviteEmail(String email, String token, String orgId, String orgName, String userName) {
        String link = buildClientUrl("/invite/accept?token=" + urlEncode(token));
        EmailTemplate template = inviteTemplate(link, orgName, userName);
        sendMail(email, template.subject(), template.text(), template.html(), null);
    }
    
    /**
     * Sends a waitlist verification email.
     */
    public void sendWaitlistVerificationEmail(String email, String code, String userName) {
        EmailTemplate template = waitlistVerificationTemplate(code, userName);
        sendMail(email, template.subject(), template.text(), template.html(), null);
    }
    
    /**
     * Sends a waitlist invite email.
     */
    public void sendWaitlistInviteEmail(String email, String registerLink, String domain, String calendly) {
        EmailTemplate template = waitlistInviteTemplate(registerLink, domain, calendly);
        sendMail(email, template.subject(), template.text(), template.html(), null);
    }
    
    /**
     * Gets sent messages (for testing).
     */
    public List<Map<String, String>> getSentMessages() {
        return sentMessages;
    }
    
    // ==================== Template Methods ====================
    
    private String buildClientUrl(String path) {
        return clientBaseUrl + path;
    }
    
    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }
    
    private String renderBrandedTemplate(String title, String bodyHtml, 
                                         String ctaLabel, String ctaHref, String footerNote) {
        String ctaButton = "";
        if (ctaLabel != null && ctaHref != null) {
            ctaButton = String.format("""
                <div style="text-align: center; margin: 30px 0;">
                  <a href="%s" style="background-color: #2563eb; color: white; padding: 12px 24px; 
                     text-decoration: none; border-radius: 6px; font-weight: 600;">%s</a>
                </div>
                """, ctaHref, ctaLabel);
        }
        
        String footer = footerNote != null 
            ? String.format("<p style=\"color: #6b7280; font-size: 12px;\">%s</p>", footerNote)
            : "";
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; 
                         background-color: #f3f4f6; margin: 0; padding: 20px;">
              <div style="max-width: 600px; margin: 0 auto; background-color: white; 
                          border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); padding: 40px;">
                <div style="text-align: center; margin-bottom: 30px;">
                  <h1 style="color: #1f2937; margin: 0; font-size: 24px;">MYTE</h1>
                </div>
                <h2 style="color: #1f2937; margin-bottom: 20px;">%s</h2>
                <div style="color: #4b5563; line-height: 1.6;">
                  %s
                </div>
                %s
                <hr style="border: none; border-top: 1px solid #e5e7eb; margin: 30px 0;">
                <div style="text-align: center;">
                  %s
                  <p style="color: #9ca3af; font-size: 12px;">
                    © %d MYTE. All rights reserved.
                  </p>
                </div>
              </div>
            </body>
            </html>
            """, title, bodyHtml, ctaButton, footer, java.time.Year.now().getValue());
    }
    
    private EmailTemplate verificationTemplate(String verifyLink, String userName) {
        String greeting = userName != null ? "Hi " + userName + "," : "Hi,";
        
        String subject = "Verify your email address";
        
        String text = String.format("""
            %s
            
            Please verify your email address to complete your registration.
            
            Click here to verify: %s
            
            If you didn't create an account, you can safely ignore this email.
            
            Best,
            The MYTE Team
            """, greeting, verifyLink);
        
        String html = renderBrandedTemplate(
            "Verify Your Email",
            String.format("""
                <p>%s</p>
                <p>Please verify your email address to complete your registration.</p>
                <p>If you didn't create an account, you can safely ignore this email.</p>
                """, greeting),
            "Verify Email",
            verifyLink,
            "This link will expire in 24 hours."
        );
        
        return new EmailTemplate(subject, text, html);
    }
    
    private EmailTemplate passwordResetTemplate(String resetLink, String userName) {
        String greeting = userName != null ? "Hi " + userName + "," : "Hi,";
        
        String subject = "Reset your password";
        
        String text = String.format("""
            %s
            
            We received a request to reset your password.
            
            Click here to reset: %s
            
            If you didn't request this, you can safely ignore this email.
            
            Best,
            The MYTE Team
            """, greeting, resetLink);
        
        String html = renderBrandedTemplate(
            "Reset Your Password",
            String.format("""
                <p>%s</p>
                <p>We received a request to reset your password.</p>
                <p>If you didn't request this, you can safely ignore this email.</p>
                """, greeting),
            "Reset Password",
            resetLink,
            "This link will expire in 1 hour."
        );
        
        return new EmailTemplate(subject, text, html);
    }
    
    private EmailTemplate inviteTemplate(String inviteLink, String orgName, String userName) {
        String greeting = userName != null ? "Hi " + userName + "," : "Hi,";
        String org = orgName != null ? orgName : "an organization";
        
        String subject = "You've been invited to join " + org;
        
        String text = String.format("""
            %s
            
            You've been invited to join %s on MYTE.
            
            Click here to accept: %s
            
            Best,
            The MYTE Team
            """, greeting, org, inviteLink);
        
        String html = renderBrandedTemplate(
            "You're Invited!",
            String.format("""
                <p>%s</p>
                <p>You've been invited to join <strong>%s</strong> on MYTE.</p>
                <p>Click the button below to accept your invitation and create your account.</p>
                """, greeting, org),
            "Accept Invitation",
            inviteLink,
            "This invitation will expire in 7 days."
        );
        
        return new EmailTemplate(subject, text, html);
    }
    
    private EmailTemplate waitlistVerificationTemplate(String code, String userName) {
        String greeting = userName != null ? "Hi " + userName + "," : "Hi,";
        
        String subject = "Your MYTE verification code: " + code;
        
        String text = String.format("""
            %s
            
            Your verification code is: %s
            
            Enter this code to verify your email address.
            
            Best,
            The MYTE Team
            """, greeting, code);
        
        String html = renderBrandedTemplate(
            "Your Verification Code",
            String.format("""
                <p>%s</p>
                <p>Your verification code is:</p>
                <div style="text-align: center; margin: 30px 0;">
                  <span style="font-size: 32px; font-weight: bold; letter-spacing: 8px; 
                               color: #1f2937; background-color: #f3f4f6; padding: 16px 24px; 
                               border-radius: 8px;">%s</span>
                </div>
                <p>Enter this code to verify your email address.</p>
                """, greeting, code),
            null,
            null,
            "This code will expire in 30 minutes."
        );
        
        return new EmailTemplate(subject, text, html);
    }
    
    private EmailTemplate waitlistInviteTemplate(String registerLink, String domain, String calendly) {
        String subject = "You're in! Welcome to MYTE";
        
        String text = String.format("""
            Great news! You've been approved to join MYTE.
            
            Click here to create your account: %s
            
            Your company domain: %s
            
            Want to learn more? Book a call: %s
            
            Best,
            The MYTE Team
            """, registerLink, domain, calendly);
        
        String html = renderBrandedTemplate(
            "Welcome to MYTE!",
            String.format("""
                <p>Great news! You've been approved to join MYTE.</p>
                <p>Your company domain <strong>%s</strong> is ready for you.</p>
                <p style="margin-top: 20px;">
                  <a href="%s" style="color: #2563eb;">Book a call</a> to learn more about 
                  how MYTE can help your team.
                </p>
                """, domain, calendly),
            "Create Your Account",
            registerLink,
            "This link will expire in 7 days."
        );
        
        return new EmailTemplate(subject, text, html);
    }
    
    /**
     * Email template record.
     */
    public record EmailTemplate(String subject, String text, String html) {}
}
