package dev.vepo.contraponto.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.auth.AccountEmailService;
import dev.vepo.contraponto.auth.PasswordService;
import dev.vepo.contraponto.navigation.NavigationHub;
import dev.vepo.contraponto.navigation.NavigationHubService;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import dev.vepo.contraponto.user.LoggedUserProvider;
import dev.vepo.contraponto.shared.i18n.I18nDefaults;
import dev.vepo.contraponto.shared.i18n.I18nKeys;
import dev.vepo.contraponto.shared.toast.Toast;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashSet;
import java.util.Set;

@Logged
@ApplicationScoped
@Path("/forms/users")
public class UserSaveEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(UserSaveEndpoint.class);

    private final UserRepository userRepository;
    private final UserAccess userAccess;
    private final UserService userService;
    private final PasswordService passwordService;
    private final NavigationHubService navigationHubService;
    private final LoggedUser loggedUser;
    private final LoggedUserProvider loggedUserProvider;
    private final AccountEmailService accountEmailService;

    @Inject
    public UserSaveEndpoint(UserRepository userRepository,
                            UserAccess userAccess,
                            UserService userService,
                            PasswordService passwordService,
                            NavigationHubService navigationHubService,
                            LoggedUser loggedUser,
                            LoggedUserProvider loggedUserProvider,
                            AccountEmailService accountEmailService) {
        this.userRepository = userRepository;
        this.userAccess = userAccess;
        this.userService = userService;
        this.passwordService = passwordService;
        this.navigationHubService = navigationHubService;
        this.loggedUser = loggedUser;
        this.loggedUserProvider = loggedUserProvider;
        this.accountEmailService = accountEmailService;
    }

    private boolean applyPasswordChange(User user, UserManageForm form) {
        var newPassword = form.getNewPassword();
        if (newPassword != null && !newPassword.isBlank()) {
            user.setPasswordHash(passwordService.hashPassword(newPassword));
            return true;
        }
        return false;
    }

    private Response badRequest(String message) {
        return Toast.response(Response.Status.BAD_REQUEST)
                    .message(message)
                    .type(Toast.Type.ERROR)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .build();
    }

    private Response create(UserManageForm form) {
        var validationError = userService.validateNewUser(form.getUsername(), form.getName(), form.getEmail(), form.getPassword());
        return validationError.map(this::badRequest).orElseGet(() -> {
            var roles = userAccess.parseRoles(loggedUser, form.getRoles());
            if (roles.isEmpty()) {
                roles = java.util.Set.of(Role.USER);
            }

            var user = userService.createUser(form.getUsername(), form.getName(), form.getEmail(), form.getPassword(), roles, true);
            logger.info("Created user {}", user);

            return Toast.ok()
                        .i18nKey(I18nKeys.TOAST_USER_CREATED, I18nDefaults.USER_CREATED)
                        .type(Toast.Type.SUCCESS)
                        .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                        .url("/administration/users")
                        .page(navigationHubService.shell(NavigationHub.ADMINISTRATION, "users", 1))
                        .build();
        });
    }

    private Response forbidden() {
        return Toast.response(Response.Status.FORBIDDEN)
                    .i18nKey(I18nKeys.TOAST_USER_FORBIDDEN, I18nDefaults.USER_FORBIDDEN)
                    .type(Toast.Type.ERROR)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .build();
    }

    private Response notFound() {
        return Toast.response(Response.Status.NOT_FOUND)
                    .i18nKey(I18nKeys.TOAST_USER_NOT_FOUND, I18nDefaults.USER_NOT_FOUND)
                    .type(Toast.Type.ERROR)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .build();
    }

    private Set<Role> resolveRoles(UserManageForm form, User user) {
        var roles = userAccess.parseRoles(loggedUser, form.getRoles());
        if (roles.isEmpty()) {
            roles = new HashSet<>(user.getRoles());
        }
        return roles;
    }

    @POST
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response save(@BeanParam UserManageForm form) {
        if (!userAccess.canManageUsers(loggedUser)) {
            return forbidden();
        }

        if (form.getId() == null) {
            return create(form);
        }
        return update(form);
    }

    private Response update(UserManageForm form) {
        var user = userRepository.findById(form.getId()).orElse(null);
        if (user == null) {
            return notFound();
        }

        var validationError = validateUpdate(form, user);
        if (validationError != null) {
            return validationError;
        }

        boolean passwordChanged = applyPasswordChange(user, form);

        user.setName(form.getName().trim());
        user.setEmail(form.getEmail().trim());
        user.setActive(form.isActive());

        var roles = resolveRoles(form, user);
        var roleError = validateRoleChange(user, roles);
        if (roleError != null) {
            return roleError;
        }

        user.setRoles(roles);
        userRepository.update(user);

        if (user.getId().equals(loggedUser.getId())) {
            loggedUserProvider.update(loggedUser.getSessionId(), user);
        }

        if (passwordChanged) {
            accountEmailService.sendPasswordChanged(user);
        }

        logger.info("Updated user {}", user);

        return Toast.ok()
                    .i18nKey(I18nKeys.TOAST_USER_SAVED, I18nDefaults.USER_SAVED)
                    .type(Toast.Type.SUCCESS)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .url("/administration/users")
                    .page(navigationHubService.shell(NavigationHub.ADMINISTRATION, "users", 1))
                    .build();
    }

    private Response validateRoleChange(User user, Set<Role> roles) {
        if (roles.isEmpty()) {
            return badRequest("Select at least one role.");
        }
        if (user.getId().equals(loggedUser.getId())
                && !roles.contains(Role.USER_ADMINISTRATOR)
                && !roles.contains(Role.ADMIN)) {
            var stillAdmin = user.getRoles().stream().anyMatch(r -> r == Role.USER_ADMINISTRATOR || r == Role.ADMIN);
            if (stillAdmin) {
                return badRequest("You cannot remove your own administrator access.");
            }
        }
        return null;
    }

    private Response validateUpdate(UserManageForm form, User user) {
        if (form.getName() == null || form.getName().isBlank()) {
            return badRequest("Name is required.");
        }
        if (form.getEmail() == null || form.getEmail().isBlank()) {
            return badRequest("Email is required.");
        }
        if (!form.getEmail().contains("@") || !form.getEmail().contains(".")) {
            return badRequest("Please enter a valid email address.");
        }
        String trimmedEmail = form.getEmail().trim();
        if (!trimmedEmail.equalsIgnoreCase(user.getEmail())
                && userRepository.existsByEmail(trimmedEmail, user.getId())) {
            return badRequest("Email already registered.");
        }
        if (user.getId().equals(loggedUser.getId()) && !form.isActive()) {
            return badRequest("You cannot deactivate your own account.");
        }
        var newPassword = form.getNewPassword();
        if (newPassword != null && !newPassword.isBlank() && newPassword.length() < 8) {
            return badRequest("Password must be at least 8 characters.");
        }
        return null;
    }
}
