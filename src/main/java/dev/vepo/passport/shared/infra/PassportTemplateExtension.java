package dev.vepo.passport.shared.infra;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import io.quarkus.qute.TemplateExtension;

@TemplateExtension
public class PassportTemplateExtension {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:MM");

    public static String formatDateTime(Instant value) {
        return FORMATTER.format(value.atZone(ZoneId.systemDefault()));
    }
}
