package dev.vepo.contraponto.image;

import java.io.IOException;
import java.io.InputStream;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.shared.infra.Logged;
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
import jakarta.ws.rs.core.Response;

@Path("/api/images")
@Produces(MediaType.APPLICATION_JSON)
public class ImageEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(ImageEndpoint.class);

    @Inject
    ImageService imageService;

    record ErrorResponse(String error) {}

    @DELETE
    @Path("/{uuid}")
    @Logged
    public Response deleteImage(@PathParam("uuid") String uuid) {
        try {
            imageService.deleteImage(uuid);
            return Response.noContent().build();
        } catch (Exception e) {
            logger.error("Failed to delete image: {}", uuid, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(new ErrorResponse("Failed to delete image"))
                           .build();
        }
    }

    @GET
    @Path("/{filename}")
    @Produces({ "image/jpeg", "image/png", "image/gif", "image/webp" })
    public Response getImage(@PathParam("filename") String filename) {
        try {
            ImageData imageData = imageService.getImage(filename);
            var cacheControl = new CacheControl();
            cacheControl.setMaxAge(31536000);
            cacheControl.setPrivate(false);
            return Response.ok(imageData.data())
                           .type(imageData.contentType())
                           .header("Content-Length", imageData.size())
                           .cacheControl(cacheControl) // Cache for 1 year
                           .build();
        } catch (IOException e) {
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

            try (InputStream data = fileUpload.filePath().toFile().toURI().toURL().openStream()) {
                return Response.ok(imageService.uploadImage(fileUpload.fileName(),
                                                            fileUpload.contentType(),
                                                            data,
                                                            fileUpload.size()))
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