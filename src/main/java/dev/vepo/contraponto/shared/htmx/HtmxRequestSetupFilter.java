package dev.vepo.contraponto.shared.htmx;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class HtmxRequestSetupFilter implements ContainerRequestFilter {

    private static final String HTMX_REQUEST_HEADER = "HX-Request";

    private final HtmxRequest htmxRequest;

    @Inject
    public HtmxRequestSetupFilter(HtmxRequest htmxRequest) {
        this.htmxRequest = htmxRequest;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        htmxRequest.setActive("true".equalsIgnoreCase(requestContext.getHeaderString(HTMX_REQUEST_HEADER)));
    }
}
