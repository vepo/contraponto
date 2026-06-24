package dev.vepo.contraponto.user;

import io.quarkus.qute.TemplateGlobal;
import jakarta.enterprise.inject.spi.CDI;

@TemplateGlobal
public class IdentityTemplateGlobals {

    @TemplateGlobal(name = "session")
    public static LoggedUser session() {
        var loggedUser = CDI.current().select(LoggedUser.class);
        if (!loggedUser.isResolvable()) {
            return new LoggedUser();
        }
        return loggedUser.get();
    }

    private IdentityTemplateGlobals() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
