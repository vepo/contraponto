package dev.vepo.contraponto.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(AuthEndpoint.class);

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;

    @Inject
    public AuthEndpoint(UserRepository userRepository,
                        PasswordService passwordService,
                        JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
    }

    @POST
    @Path("/signup")
    public Response signup(@Valid SignupRequest request) {
        try {
            // Check if user already exists
            if (userRepository.existsByEmail(request.email())) {
                return Response.status(Response.Status.CONFLICT)
                               .entity(new ErrorResponse("Email already registered"))
                               .build();
            }

            // Create new user
            String hashedPassword = passwordService.hashPassword(request.password());
            User user = new User(request.email(), request.name(), hashedPassword);
            userRepository.save(user);

            // Generate tokens
            String token = jwtService.generateToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            // Prepare response
            AuthResponse response = new AuthResponse(
                                                     token,
                                                     refreshToken,
                                                     new AuthResponse.UserInfo(user.getId(), user.getName(), user.getEmail()));

            logger.info("User registered successfully: " + user.getEmail());
            return Response.ok(response).build();
        } catch (Exception e) {
            logger.error("Error during signup!", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(new ErrorResponse("Error creating user"))
                           .build();
        }
    }

    @POST
    @Path("/login")
    public Response login(@Valid AuthRequest request) {
        try {
            // Find user by email
            User user = userRepository.findByEmail(request.getEmail())
                                      .orElse(null);

            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                               .entity(new ErrorResponse("Invalid credentials"))
                               .build();
            }

            // Check password
            if (!passwordService.verifyPassword(request.getPassword(), user.getPasswordHash())) {
                return Response.status(Response.Status.UNAUTHORIZED)
                               .entity(new ErrorResponse("Invalid credentials"))
                               .build();
            }

            // Check if user is active
            if (!user.isActive()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                               .entity(new ErrorResponse("Account is disabled"))
                               .build();
            }

            // Generate tokens
            String token = jwtService.generateToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            // Prepare response
            AuthResponse response = new AuthResponse(
                                                     token,
                                                     refreshToken,
                                                     new AuthResponse.UserInfo(user.getId(), user.getName(), user.getEmail()));

            logger.info("User logged in: " + user.getEmail());
            return Response.ok(response).build();
        } catch (Exception e) {
            logger.error("Error during login!", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(new ErrorResponse("Error during login"))
                           .build();
        }
    }

    @POST
    @Path("/logout")
    public Response logout() {
        // In a real implementation, you might want to invalidate the token
        // Since JWT is stateless, just return success
        return Response.ok(new SuccessResponse("Logged out successfully")).build();
    }

    // Error and Success response classes
    public static class ErrorResponse {
        private String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    public static class SuccessResponse {
        private String message;

        public SuccessResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}