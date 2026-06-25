package dev.vepo.contraponto.components;

import dev.vepo.contraponto.user.LoggedUser;
import io.quarkus.qute.TemplateGlobal;
import jakarta.enterprise.inject.spi.CDI;

@TemplateGlobal
public class MenuTemplateGlobals {

    @TemplateGlobal(name = "menuNav")
    public static MenuNavigation menuNav() {
        var session = CDI.current().select(LoggedUser.class);
        var user = session.isResolvable() ? session.get() : new LoggedUser();
        return CDI.current().select(MenuNavigationService.class).get().build(user);
    }

    private MenuTemplateGlobals() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
