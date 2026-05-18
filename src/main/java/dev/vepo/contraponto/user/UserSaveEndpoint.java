package dev.vepo.contraponto.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.auth.PasswordService;
import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.infra.LoggedUserProvider;
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
    private final CustomPageRepository customPageRepository;
    private final UserManageEndpoint userManageEndpoint;
    private final LoggedUser loggedUser;
    private final LoggedUserProvider loggedUserProvider;

    @Inject
    public UserSaveEndpoint(UserRepository userRepository,
                            UserAccess userAccess,
                            UserService userService,
                            PasswordService passwordService,
                            CustomPageRepository customPageRepository,
                            UserManageEndpoint userManageEndpoint,
                            LoggedUser loggedUser,
                            LoggedUserProvider loggedUserProvider) {
        this.userRepository = userRepository;
        this.userAccess = userAccess;
        this.userService = userService;
        this.passwordService = passwordService;
        this.customPageRepository = customPageRepository;
        this.userManageEndpoint = userManageEndpoint;
        this.loggedUser = loggedUser;
        this.loggedUserProvider = loggedUserProvider;
    }

    private void applyPasswordChange(User user, UserManageForm form) {
        var newPassword = form.getNewPassword();
        if (newPassword != null && !newPassword.isBlank()) {
            user.setPasswordHash(passwordService.hashPassword(newPassword));
        }
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
        if (validationError.isPresent()) {
            return badRequest(validationError.get());
        }

        var roles = userAccess.parseRoles(loggedUser, form.getRoles());
        if (roles.isEmpty()) {
            roles = java.util.Set.of(Role.USER);
        }

        var user = userService.createUser(form.getUsername(), form.getName(), form.getEmail(), form.getPassword(), roles);
        logger.info("Created user id={} username={}", user.getId(), user.getUsername());

        return Toast.ok()
                    .message("User created successfully.")
                    .type(Toast.Type.SUCCESS)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .url("/users")
                    .page(UserManageEndpoint.Templates.list(userManageEndpoint.listPage(1),
                                                            customPageRepository.loadLinks(),
                                                            loggedUser))
                    .build();
    }

    private Response forbidden() {
        return Toast.response(Response.Status.FORBIDDEN)
                    .message("You do not have permission to manage users.")
                    .type(Toast.Type.ERROR)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .build();
    }

    private Response notFound() {
        return Toast.response(Response.Status.NOT_FOUND)
                    .message("User not found.")
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

        applyPasswordChange(user, form);

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

        logger.info("Updated user id={} username={}", user.getId(), user.getUsername());

        return Toast.ok()
                    .message("User saved successfully.")
                    .type(Toast.Type.SUCCESS)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .url("/users")
                    .page(UserManageEndpoint.Templates.list(userManageEndpoint.listPage(1),
                                                            customPageRepository.loadLinks(),
                                                            loggedUser))
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
        if (userRepository.existsByEmail(form.getEmail().trim(), user.getId())) {
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
