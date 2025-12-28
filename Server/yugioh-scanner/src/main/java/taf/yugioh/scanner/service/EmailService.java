package taf.yugioh.scanner.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * Send password reset email with token
     */
    public void sendPasswordResetEmail(String toEmail, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Yu-Gi-Oh! Scanner - Password Reset Request");
        message.setText(
                "Hello,\n\n" +
                        "We received a request to reset your password for your Yu-Gi-Oh! Scanner account.\n\n" +
                        "Click the link below to reset your password:\n" +
                        resetLink + "\n\n" +
                        "This link will expire in 1 hour.\n\n" +
                        "If you did not request a password reset, please ignore this email. " +
                        "Your password will remain unchanged.\n\n" +
                        "Best regards,\n" +
                        "Yu-Gi-Oh! Scanner Team"
        );

        mailSender.send(message);
    }

    /**
     * Send username reminder email
     */
    public void sendUsernameReminderEmail(String toEmail, String username) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Yu-Gi-Oh! Scanner - Your Username");
        message.setText(
                "Hello,\n\n" +
                        "You requested a reminder of your username for Yu-Gi-Oh! Scanner.\n\n" +
                        "Your username is: " + username + "\n\n" +
                        "You can log in here: " + frontendUrl + "/login\n\n" +
                        "If you did not request this, please ignore this email.\n\n" +
                        "Best regards,\n" +
                        "Yu-Gi-Oh! Scanner Team"
        );

        mailSender.send(message);
    }

    /**
     * Send password changed confirmation email
     */
    public void sendPasswordChangedEmail(String toEmail, String username) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Yu-Gi-Oh! Scanner - Password Changed Successfully");
        message.setText(
                "Hello " + username + ",\n\n" +
                        "Your password has been successfully changed.\n\n" +
                        "If you did not make this change, please contact support immediately.\n\n" +
                        "Best regards,\n" +
                        "Yu-Gi-Oh! Scanner Team"
        );

        mailSender.send(message);
    }
}