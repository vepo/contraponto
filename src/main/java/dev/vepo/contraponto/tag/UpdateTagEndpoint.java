package dev.vepo.contraponto.tag;

import dev.vepo.contraponto.shared.Slug;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import dev.vepo.contraponto.shared.i18n.I18nDefaults;
import dev.vepo.contraponto.shared.i18n.I18nKeys;
import dev.vepo.contraponto.shared.toast.Toast;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Logged
@ApplicationScoped
@Path("/forms/tags/update")
public class UpdateTagEndpoint {

    private static final String ERROR_NAME = "Tag name is required.";
    private static final String ERROR_SLUG = "Tag URL slug is invalid. Use lowercase letters, numbers, and hyphens only.";
    private static final String ERROR_SLUG_TAKEN = "That slug is already used by another tag.";
    private static final String SUCCESS = "Tag updated.";

    private final TagRepository tagRepository;
    private final LoggedUser loggedUser;

    @Inject
    public UpdateTagEndpoint(TagRepository tagRepository, LoggedUser loggedUser) {
        this.tagRepository = tagRepository;
        this.loggedUser = loggedUser;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @POST
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response update(@BeanParam UpdateTagForm form) {
        if (!loggedUser.isEditor()) {
            return Response.status(Status.FORBIDDEN).build();
        }
        if (form.tagId() == null) {
            return Toast.response(Status.BAD_REQUEST).i18nKey(I18nKeys.TOAST_TAG_MISSING, I18nDefaults.TAG_MISSING).type(Toast.Type.ERROR).build();
        }
        Tag tag = tagRepository.findById(form.tagId()).orElseThrow(() -> new NotFoundException("Tag not found"));
        if (isBlank(form.name())) {
            return Toast.response(Status.BAD_REQUEST).i18nKey(I18nKeys.TOAST_TAG_NAME_REQUIRED, I18nDefaults.TAG_NAME_REQUIRED).type(Toast.Type.ERROR).build();
        }
        if (isBlank(form.slug())) {
            return Toast.response(Status.BAD_REQUEST).i18nKey(I18nKeys.TOAST_TAG_SLUG_INVALID, I18nDefaults.TAG_SLUG_INVALID).type(Toast.Type.ERROR).build();
        }
        if (Slug.hasInvalidSlugCharacters(form.slug().trim())) {
            return Toast.response(Status.BAD_REQUEST).i18nKey(I18nKeys.TOAST_TAG_SLUG_INVALID, I18nDefaults.TAG_SLUG_INVALID).type(Toast.Type.ERROR).build();
        }
        String newSlug = Slug.slugify(form.slug().trim());
        if (newSlug.isEmpty()) {
            return Toast.response(Status.BAD_REQUEST).i18nKey(I18nKeys.TOAST_TAG_SLUG_INVALID, I18nDefaults.TAG_SLUG_INVALID).type(Toast.Type.ERROR).build();
        }
        if (tagRepository.existsOtherWithSlug(tag.getId(), newSlug)) {
            return Toast.response(Status.BAD_REQUEST).i18nKey(I18nKeys.TOAST_TAG_SLUG_TAKEN, I18nDefaults.TAG_SLUG_TAKEN).type(Toast.Type.ERROR).build();
        }
        tag.setSlug(newSlug);
        tag.setName(form.name().trim());
        if (isBlank(form.description())) {
            tag.setDescription(null);
        } else {
            tag.setDescription(form.description().trim());
        }
        tagRepository.save(tag);
        return Toast.ok()
                    .i18nKey(I18nKeys.TOAST_TAG_UPDATED, I18nDefaults.TAG_UPDATED)
                    .type(Toast.Type.SUCCESS)
                    .url(TagPaths.url(tag))
                    .build();
    }
}
