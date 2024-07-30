package io.lcalmsky.app.infra.email;

import org.springframework.stereotype.Service;

public interface EmailService {
    void sendEmail(EmailMessage emailMessage);
}
