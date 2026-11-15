package com.cuymonitor.backend.adapter.out.mail;

import com.cuymonitor.backend.domain.port.out.OtpSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
@Profile("!dev")
public class EmailOtpSender implements OtpSender {

    private final JavaMailSender mailSender;
    private final String from;
    private final Clock clock;

    public EmailOtpSender(JavaMailSender mailSender, @Value("${app.mail.from}") String from, Clock clock) {
        this.mailSender = mailSender;
        this.clock = clock;
        this.from = from;
    }

    @Override
    public void sendOtp(String email, String code, Instant expiresAt) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("Tu código de verificación - Cuy Monitor");
        message.setText("""
                Hola,

                Tu código de verificación es: %s

                Vence en %d minutos. Si no fuiste tú, ignora este correo.
                """.formatted(code, minutesLeft(expiresAt)));
        mailSender.send(message);
    }

    private long minutesLeft(Instant expiresAt) {
        long seconds = Duration.between(clock.instant(), expiresAt).toSeconds();
        return Math.max(1, (seconds + 59) / 60);
    }
}
