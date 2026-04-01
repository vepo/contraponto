package dev.vepo.contraponto.user;

import org.eclipse.microprofile.jwt.JsonWebToken;

import dev.vepo.contraponto.auth.AuthResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/profile")
@Produces(MediaType.APPLICATION_JSON)
public class ProfileEndpoint {

    @Inject
    JsonWebToken jwt;

    @Inject
    UserRepository userRepository;

    @GET
    public Response getProfile() {
        String userId = jwt.getSubject();
        User user = userRepository.findById(Long.parseLong(userId));

        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(new AuthResponse.UserInfo(user.getId(), user.getName(), user.getEmail())).build();
    }
}