package dev.vepo.passport.mailer;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.mailer.MailTemplate.MailTemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;

@ApplicationScoped
public class MailerService {

    private static final Logger logger = LoggerFactory.getLogger(MailerService.class);

    record userCreated(String baseUrl, UserCreatedEvent event) implements MailTemplateInstance {}

    record resetTokenCreated(String baseUrl, ResetPasswordCreatedEvent event) implements MailTemplateInstance {}

    private final String baseUrl;

    @Inject
    public MailerService(@ConfigProperty(name = "base.url") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void userCreated(@ObservesAsync UserCreatedEvent event) {
        logger.info("Sending email to user! username={}", event.username());
        new userCreated(baseUrl, event).to(event.email())
                                       .subject("[BACKOFFICE] Usuário criado!")
                                       .send()
                                       .subscribe()
                                       .with(success -> logger.info("Created user email sent to {}", event.email()),
                                             failure -> logger.error("Failed to send created user email to {}", event.email(), failure));
    }

    public void resetTokenCreated(@ObservesAsync ResetPasswordCreatedEvent event) {
        logger.info("Sending email to user! username={}", event.username());
        new resetTokenCreated(baseUrl, event).to(event.email())
                                             .subject("[BACKOFFICE] Recuperação de senha requisitado!")
                                             .send()
                                             .subscribe()
                                             .with(success -> logger.info("Password reset email sent to {}", event.email()),
                                                   failure -> logger.error("Failed to send password reset email to {}", event.email(), failure));
    }
}