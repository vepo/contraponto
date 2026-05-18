package dev.vepo.contraponto.components.forms;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.auth.AccountEmailService;
import dev.vepo.contraponto.auth.EmailVerificationService;
import dev.vepo.contraponto.auth.PasswordService;
import dev.vepo.contraponto.blog.BlogBannerService;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.infra.LoggedUserProvider;
import dev.vepo.contraponto.user.UserRepository;
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
import jakarta.ws.rs.core.Response.Status;

@Logged
@ApplicationScoped
@Path("/forms/profile")
public class ProfileUpdateEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(ProfileUpdateEndpoint.class);

    private final LoggedUser loggedUser;
    private final UserRepository userRepository;
    private final LoggedUserProvider loggedUserProvider;
    private final PasswordService passwordService;
    private final BlogBannerService blogBannerService;
    private final EmailVerificationService emailVerificationService;
    private final AccountEmailService accountEmailService;

    @Inject
    public ProfileUpdateEndpoint(LoggedUser loggedUser,
                                 LoggedUserProvider loggedUserProvider,
                                 UserRepository userRepository,
                                 PasswordService passwordService,
                                 BlogBannerService blogBannerService,
                                 EmailVerificationService emailVerificationService,
                                 AccountEmailService accountEmailService) {
        this.loggedUser = loggedUser;
        this.loggedUserProvider = loggedUserProvider;
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.blogBannerService = blogBannerService;
        this.emailVerificationService = emailVerificationService;
        this.accountEmailService = accountEmailService;
    }

    private String buildErrorResponseBody(String message) {
        return String.format("""
                             <div class="error-message visible">%s</div>
                             """, message);
    }

    private String buildSuccessResponseBody(String message) {
        return String.format("""
                             <div class="success-message">%s</div>
                             """, message);
    }

    @POST
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response login(@BeanParam ProfileUpdateRequest request) {
        logger.info("Updating profile for user id={}", loggedUser.getId());
        if (!loggedUser.isAuthenticated()) {
            return Response.status(Status.FORBIDDEN)
                           .build();
        }

        var mabyeUser = userRepository.findById(loggedUser.getId());
        if (mabyeUser.isEmpty()) {
            return Response.status(Status.NOT_FOUND)
                           .build();
        }

        var user = mabyeUser.get();
        if (!passwordService.verifyPassword(request.currentPassword(), user.getPasswordHash())) {
            return Response.ok(buildErrorResponseBody("Current password is incorrect"))
                           .build();
        }

        boolean updated = false;
        boolean passwordChanged = false;
        String requestedEmail = request.email().trim();

        if (!requestedEmail.equals(user.getEmail())) {
            if (userRepository.existsByEmail(requestedEmail, user.getId())) {
                return Response.ok(buildErrorResponseBody("Email already registered"))
                               .build();
            }
            emailVerificationService.requestEmailChange(user, requestedEmail);
            return Response.ok(buildSuccessResponseBody("Check your new email to confirm the address change."))
                           .build();
        }

        if (user.getPendingEmail() != null) {
            user.setPendingEmail(null);
            updated = true;
        }

        if (!request.name().equals(user.getName())) {
            user.setName(request.name());
            updated = true;
        }

        if (Objects.nonNull(request.newPassword()) && !request.newPassword().isBlank()) {
            if (!request.newPassword().equals(request.confirmPassword())) {
                return Response.ok(buildErrorResponseBody("Passwords do not match"))
                               .build();
            }
            if (request.newPassword().length() < 8) {
                return Response.ok(buildErrorResponseBody("Password must be at least 8 characters."))
                               .build();
            }

            user.setPasswordHash(passwordService.hashPassword(request.newPassword()));
            updated = true;
            passwordChanged = true;
        }

        if (request.profilePictureId() != null) {
            blogBannerService.applyProfilePicture(user, request.profilePictureId());
            updated = true;
        }

        if (request.defaultBannerId() != null) {
            blogBannerService.applyDefaultBlogBanner(user, request.defaultBannerId());
            updated = true;
        }

        if (updated) {
            this.userRepository.update(user);
            loggedUserProvider.update(loggedUser.getSessionId(), user);
        }

        if (passwordChanged) {
            accountEmailService.sendPasswordChanged(user);
        }

        return Response.ok(buildSuccessResponseBody("Profile updated.")).build();
    }
}
