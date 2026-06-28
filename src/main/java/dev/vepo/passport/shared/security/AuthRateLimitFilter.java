package dev.vepo.passport.shared.security;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthRateLimitFilter implements ContainerRequestFilter {

    private static final Set<String> RATE_LIMITED_SUFFIXES = Set.of("auth/login",
                                                                    "auth/request-reset-password",
                                                                    "auth/reset");

    private final AuthRateLimiter authRateLimiter;
    private final boolean enabled;
    private final int maxRequests;
    private final Duration windowSize;

    public AuthRateLimitFilter(AuthRateLimiter authRateLimiter,
                               @ConfigProperty(name = "passport.auth.rate-limit.enabled", defaultValue = "true") boolean enabled,
                               @ConfigProperty(name = "passport.auth.rate-limit.max-requests", defaultValue = "30") int maxRequests,
                               @ConfigProperty(name = "passport.auth.rate-limit.window") Duration windowSize) {
        this.authRateLimiter = authRateLimiter;
        this.enabled = enabled;
        this.maxRequests = maxRequests;
        this.windowSize = windowSize;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!enabled || !isRateLimitedPath(requestContext)) {
            return;
        }

        var clientKey = resolveClientKey(requestContext);
        if (!authRateLimiter.allow(clientKey, maxRequests, windowSize)) {
            requestContext.abortWith(Response.status(Response.Status.TOO_MANY_REQUESTS)
                                             .entity("Too many authentication requests")
                                             .build());
        }
    }

    private boolean isRateLimitedPath(ContainerRequestContext requestContext) {
        var path = requestContext.getUriInfo().getPath();
        if (path == null) {
            return false;
        }
        var normalized = path.startsWith("/") ? path.substring(1) : path;
        return RATE_LIMITED_SUFFIXES.stream().anyMatch(suffix -> normalized.equals(suffix) || normalized.endsWith("/" + suffix));
    }

    private String resolveClientKey(ContainerRequestContext requestContext) {
        var forwardedFor = requestContext.getHeaderString("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            var firstHop = forwardedFor.split(",")[0].trim();
            if (!firstHop.isBlank()) {
                return firstHop;
            }
        }
        var remoteAddress = requestContext.getHeaderString("X-Real-IP");
        if (remoteAddress != null && !remoteAddress.isBlank()) {
            return remoteAddress.trim();
        }
        return "unknown";
    }

}
