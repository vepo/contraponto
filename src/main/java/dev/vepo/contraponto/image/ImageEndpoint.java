package dev.vepo.contraponto.image;

import java.io.IOException;
import java.io.InputStream;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.user.UserRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@Path("/api/images")
@Produces(MediaType.APPLICATION_JSON)
public class ImageEndpoint {

    record ErrorResponse(String error) {}

    private static final Logger logger = LoggerFactory.getLogger(ImageEndpoint.class);
    private final ImageService imageService;
    private final LoggedUser loggedUser;
    private final UserRepository userRepository;

    @Inject
    public ImageEndpoint(ImageService imageService, LoggedUser loggedUser, UserRepository userRepository) {
        this.imageService = imageService;
        this.loggedUser = loggedUser;
        this.userRepository = userRepository;
    }

    @DELETE
    @Path("/{uuid}")
    @Logged
    public Response deleteImage(@PathParam("uuid") String uuid) {
        try {
            imageService.deleteImage(uuid, loggedUser.getId());
            return Response.noContent().build();
        } catch (WebApplicationException e) {
            return Response.status(e.getResponse().getStatus())
                           .entity(new ErrorResponse(e.getMessage()))
                           .build();
        } catch (Exception e) {
            logger.error("Failed to delete image: {}", uuid, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(new ErrorResponse("Failed to delete image"))
                           .build();
        }
    }

    @GET
    @Path("/{filename}")
    @Produces({ "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml" })
    public Response getImage(@PathParam("filename") String filename) {
        try {
            ImageData imageData = imageService.getImage(filename);
            var cacheControl = new CacheControl();
            cacheControl.setMaxAge(31536000);
            cacheControl.setPrivate(false);
            return Response.ok(imageData.data())
                           .type(imageData.contentType())
                           .header("Content-Length", imageData.size())
                           .cacheControl(cacheControl)
                           .build();
        } catch (WebApplicationException e) {
            return Response.status(e.getResponse().getStatus()).build();
        } catch (Exception e) {
            logger.error("Failed to get image: {}", filename, e);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @POST
    @Logged
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadImage(@RestForm("file") FileUpload fileUpload) {
        try {
            if (fileUpload == null || fileUpload.fileName() == null || fileUpload.fileName().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                               .entity(new ErrorResponse("File is required"))
                               .build();
            }

            var owner = userRepository.findById(loggedUser.getId()).orElse(null);
            if (owner == null) {
                return Response.status(Response.Status.FORBIDDEN)
                               .entity(new ErrorResponse("Access denied"))
                               .build();
            }
            try (InputStream data = fileUpload.filePath().toFile().toURI().toURL().openStream()) {
                return Response.ok(imageService.uploadImage(fileUpload.fileName(),
                                                            fileUpload.contentType(),
                                                            data,
                                                            fileUpload.size(),
                                                            owner,
                                                            owner))
                               .build();
            }
        } catch (IOException e) {
            logger.error("Failed to upload image", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(new ErrorResponse("Failed to upload image"))
                           .build();
        }
    }
}
