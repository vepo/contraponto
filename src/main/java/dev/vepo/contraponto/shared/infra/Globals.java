package dev.vepo.contraponto.shared.infra;

import java.time.LocalDateTime;

import io.quarkus.qute.TemplateGlobal;

@TemplateGlobal
public class Globals {

    @TemplateGlobal(name = "currentYear")
    public static int currentYear() {
        return LocalDateTime.now().getYear();
    }
}
