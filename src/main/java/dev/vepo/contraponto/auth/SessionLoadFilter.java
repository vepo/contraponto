package dev.vepo.contraponto.auth;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class SessionLoadFilter implements  ContainerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(SessionLoadFilter.class);
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        logger.info("Filtering {}", requestContext.getUriInfo());
        requestContext.getCookies().forEach((key, value) -> logger.info("Cookie {}={}", key, value));
    }
}
