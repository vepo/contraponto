package dev.vepo.contraponto.user;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.navigation.BreadcrumbService;
import dev.vepo.contraponto.navigation.BreadcrumbTrail;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import dev.vepo.contraponto.shared.toast.Toast;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Logged
@Path("/users")
@ApplicationScoped
public class UserManageEndpoint {

    @CheckedTemplate
    public static class Templates {
        static native TemplateInstance form(User account,
                                            boolean creating,
                                            List<Role> assignableRoles,
                                            Links links,
                                            LoggedUser user,
                                            BreadcrumbTrail breadcrumb);

        static native TemplateInstance panel(Page<UserRow> users, String basePath);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final UserRepository userRepository;
    private final UserAccess userAccess;
    private final CustomPageRepository customPageRepository;
    private final LoggedUser loggedUser;
    private final BreadcrumbService breadcrumbService;

    @Inject
    public UserManageEndpoint(UserRepository userRepository,
                              UserAccess userAccess,
                              CustomPageRepository customPageRepository,
                              LoggedUser loggedUser,
                              BreadcrumbService breadcrumbService) {
        this.userRepository = userRepository;
        this.userAccess = userAccess;
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
        this.breadcrumbService = breadcrumbService;
    }

    @GET
    @Path("{id}/edit")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public Response edit(@PathParam("id") long id) {
        if (!userAccess.canManageUsers(loggedUser)) {
            return forbidden();
        }

        var user = userRepository.findById(id).orElseThrow(NotFoundException::new);
        return Response.ok(Templates.form(user,
                                          false,
                                          userAccess.assignableRoles(loggedUser),
                                          customPageRepository.loadLinks(),
                                          loggedUser,
                                          breadcrumbService.administrationUserForm(user.getName())))
                       .build();
    }

    private Response forbidden() {
        return Toast.response(Response.Status.FORBIDDEN)
                    .message("You do not have permission to manage users.")
                    .type(Toast.Type.ERROR)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .build();
    }

    Page<UserRow> listPage(int page) {
        return userRepository.findPageForManagement(PageQuery.forGrid(20, page))
                             .map(UserRow::from);
    }

    @GET
    @Path("new")
    @Produces(MediaType.TEXT_HTML)
    public Response newUser() {
        if (!userAccess.canManageUsers(loggedUser)) {
            return forbidden();
        }

        var account = new User();
        account.setRoles(java.util.Set.of(Role.USER));
        return Response.ok(Templates.form(account,
                                          true,
                                          userAccess.assignableRoles(loggedUser),
                                          customPageRepository.loadLinks(),
                                          loggedUser,
                                          breadcrumbService.administrationUserForm("New User")))
                       .build();
    }

    public TemplateInstance renderHubPanel(int page, String basePath) {
        return Templates.panel(listPage(page), basePath);
    }
}
