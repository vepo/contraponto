package dev.vepo.contraponto.shared.infra;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import io.quarkus.qute.TemplateExtension;

@TemplateExtension
public class Templates {

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static String formatDate(LocalDateTime date) {
        return date.format(formatter);
    }
}