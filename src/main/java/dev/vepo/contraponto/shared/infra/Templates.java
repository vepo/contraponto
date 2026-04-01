package dev.vepo.contraponto.shared.infra;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import io.quarkus.qute.TemplateExtension;

@TemplateExtension
public class Templates {
    public static String truncate(String data, int limit) {
        if (Objects.nonNull(data) && data.length() > limit) {
            return data.substring(0, limit);
        } else {
            return data;
        }
    }

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static String formatDate(LocalDateTime date) {
        return date.format(formatter);
    }
}