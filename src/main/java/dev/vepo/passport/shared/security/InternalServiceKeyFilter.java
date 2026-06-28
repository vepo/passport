package dev.vepo.passport.shared.security;

import java.io.IOException;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class InternalServiceKeyFilter implements ContainerRequestFilter {

    public static final String SERVICE_KEY_HEADER = "X-Service-Key";

    private final Optional<String> serviceKey;

    public InternalServiceKeyFilter(@ConfigProperty(name = "passport.internal.service-key") Optional<String> serviceKey) {
        this.serviceKey = serviceKey;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        var path = requestContext.getUriInfo().getPath();
        if (path == null || !path.startsWith("internal/")) {
            return;
        }

        var configuredKey = serviceKey.filter(key -> !key.isBlank());
        if (configuredKey.isEmpty()) {
            requestContext.abortWith(Response.status(Response.Status.SERVICE_UNAVAILABLE)
                                             .entity("Internal service key is not configured")
                                             .build());
            return;
        }

        var providedKey = requestContext.getHeaderString(SERVICE_KEY_HEADER);
        if (providedKey == null || !configuredKey.get().equals(providedKey)) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                                             .entity("Invalid or missing service key")
                                             .build());
        }
    }
}
