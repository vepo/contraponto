package dev.vepo.contraponto.components.forms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

import dev.vepo.contraponto.auth.PasswordService;
import dev.vepo.contraponto.blog.BlogBannerService;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import dev.vepo.contraponto.user.LoggedUserProvider;
import dev.vepo.contraponto.user.AuthorSocialUrls;
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
@Path("/forms/writing/appearance")
public class AuthorAppearanceUpdateEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(AuthorAppearanceUpdateEndpoint.class);

    private final LoggedUser loggedUser;
    private final UserRepository userRepository;
    private final LoggedUserProvider loggedUserProvider;
    private final PasswordService passwordService;
    private final BlogBannerService blogBannerService;
    private final BlogRepository blogRepository;

    @Inject
    public AuthorAppearanceUpdateEndpoint(LoggedUser loggedUser,
                                          LoggedUserProvider loggedUserProvider,
                                          UserRepository userRepository,
                                          PasswordService passwordService,
                                          BlogBannerService blogBannerService,
                                          BlogRepository blogRepository) {
        this.loggedUser = loggedUser;
        this.loggedUserProvider = loggedUserProvider;
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.blogBannerService = blogBannerService;
        this.blogRepository = blogRepository;
    }

    private String applySocialUrl(String raw, Consumer<String> setter) {
        if (raw == null) {
            return null;
        }
        if (raw.isBlank()) {
            setter.accept(null);
            return null;
        }
        var normalized = AuthorSocialUrls.normalizeHttpsUrl(raw);
        if (normalized.isEmpty()) {
            return "URL is invalid.";
        }
        String value = normalized.get();
        if (!value.startsWith("https://")) {
            return value;
        }
        setter.accept(value);
        return null;
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
    public Response update(@BeanParam AuthorAppearanceUpdateRequest request) {
        logger.info("Updating author appearance for user {}", loggedUser);
        if (!loggedUser.isAuthenticated()) {
            return Response.status(Status.FORBIDDEN).build();
        }

        var maybeUser = userRepository.findById(loggedUser.getId());
        if (maybeUser.isEmpty()) {
            return Response.status(Status.NOT_FOUND).build();
        }

        var user = maybeUser.get();
        boolean updated = false;
        boolean nameChange = request.name() != null && !request.name().equals(user.getName());

        if (nameChange) {
            if (request.currentPassword() == null
                    || request.currentPassword().isBlank()
                    || !passwordService.verifyPassword(request.currentPassword(), user.getPasswordHash())) {
                return Response.ok(buildErrorResponseBody("Current password is required to change your display name."))
                               .build();
            }
            var previousName = user.getName();
            var newName = request.name().trim();
            user.setName(newName);
            blogRepository.findMainByOwnerId(user.getId()).ifPresent(mainBlog -> {
                if (mainBlog.getName().trim().equals(previousName.trim())) {
                    mainBlog.setName(newName);
                }
            });
            updated = true;
        }

        if (request.profilePictureId() != null) {
            blogBannerService.applyProfilePicture(user, request.profilePictureId());
            updated = true;
        }

        if (request.defaultBannerId() != null) {
            blogBannerService.applyDefaultBlogBanner(user, request.defaultBannerId());
            updated = true;
        }

        if (request.profileDescription() != null) {
            user.setProfileDescription(request.profileDescription().trim());
            updated = true;
        }

        var socialError = applySocialUrl(request.websiteUrl(), user::setWebsiteUrl);
        if (socialError != null) {
            return Response.ok(buildErrorResponseBody(socialError)).build();
        }
        socialError = applySocialUrl(request.twitterUrl(), user::setTwitterUrl);
        if (socialError != null) {
            return Response.ok(buildErrorResponseBody(socialError)).build();
        }
        socialError = applySocialUrl(request.mastodonUrl(), user::setMastodonUrl);
        if (socialError != null) {
            return Response.ok(buildErrorResponseBody(socialError)).build();
        }
        socialError = applySocialUrl(request.blueskyUrl(), user::setBlueskyUrl);
        if (socialError != null) {
            return Response.ok(buildErrorResponseBody(socialError)).build();
        }
        socialError = applySocialUrl(request.githubUrl(), user::setGithubUrl);
        if (socialError != null) {
            return Response.ok(buildErrorResponseBody(socialError)).build();
        }
        socialError = applySocialUrl(request.linkedinUrl(), user::setLinkedinUrl);
        if (socialError != null) {
            return Response.ok(buildErrorResponseBody(socialError)).build();
        }
        if (request.websiteUrl() != null || request.twitterUrl() != null || request.mastodonUrl() != null
                || request.blueskyUrl() != null || request.githubUrl() != null || request.linkedinUrl() != null) {
            updated = true;
        }

        if (updated) {
            userRepository.update(user);
            loggedUserProvider.update(loggedUser.getSessionId(), user);
        }

        return Response.ok(buildSuccessResponseBody("Appearance updated.")).build();
    }
}
