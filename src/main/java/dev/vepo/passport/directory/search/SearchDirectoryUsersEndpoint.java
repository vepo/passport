package dev.vepo.passport.directory.search;

import dev.vepo.passport.directory.DirectoryPageResponse;
import dev.vepo.passport.directory.DirectoryUserResponse;
import dev.vepo.passport.user.UserRepository;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/directory/users")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class SearchDirectoryUsersEndpoint {

    private static final int MIN_QUERY_LENGTH = 2;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final UserRepository userRepository;

    @Inject
    public SearchDirectoryUsersEndpoint(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GET
    public Response search(@QueryParam("q") String q,
                           @QueryParam("page") @DefaultValue("0") int page,
                           @QueryParam("size") @DefaultValue("20") int size) {
        if (q == null || q.isBlank() || q.trim().length() < MIN_QUERY_LENGTH) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(new ErrorBody("Query must be at least %d characters".formatted(MIN_QUERY_LENGTH)))
                           .build();
        }
        if (page < 0) {
            page = 0;
        }
        if (size < 1) {
            size = DEFAULT_SIZE;
        }
        if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }
        var query = q.trim();
        var total = userRepository.countDirectory(query);
        var items = userRepository.searchDirectory(query, page, size)
                                  .stream()
                                  .map(DirectoryUserResponse::load)
                                  .toList();
        return Response.ok(new DirectoryPageResponse(items, page, size, total)).build();
    }

    public record ErrorBody(String message) {}
}
