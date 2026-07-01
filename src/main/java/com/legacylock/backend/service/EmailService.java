package com.legacylock.backend.service;

import com.legacylock.backend.exceptions.LegacyLockException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public void sendSimpleEmail(String to, String subject, String body) {
        if (!mailEnabled) {
            log.info("Mail disabled. Would send email to {} with subject {}", to, subject);
            log.info("Email body: {}", body);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            javaMailSender.send(message);

            log.info("Email sent successfully to {}", to);

        } catch (MailException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new LegacyLockException("Could not send email");
        }
    }

    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        if (!mailEnabled) {
            log.info("Mail disabled. Would send HTML email to {} with subject {}", to, subject);
            log.info("HTML email body: {}", htmlBody);
            return;
        }

        try {
            MimeMessage message = javaMailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    "UTF-8"
            );

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            javaMailSender.send(message);

            log.info("HTML email sent successfully to {}", to);

        } catch (Exception e) {
            log.error("Failed to send HTML email to {}: {}", to, e.getMessage());
            throw new LegacyLockException("Could not send HTML email");
        }
    }
}
