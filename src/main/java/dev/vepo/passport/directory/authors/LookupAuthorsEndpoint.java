package dev.vepo.passport.directory.authors;

import java.util.LinkedHashSet;
import java.util.List;

import dev.vepo.passport.directory.LookupAuthorsRequest;
import dev.vepo.passport.directory.PublicAuthorResponse;
import dev.vepo.passport.user.UserRepository;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/directory/authors")
@ApplicationScoped
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LookupAuthorsEndpoint {

    private final UserRepository userRepository;

    @Inject
    public LookupAuthorsEndpoint(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @POST
    public List<PublicAuthorResponse> lookup(@Valid LookupAuthorsRequest request) {
        var uniqueIds = new LinkedHashSet<>(request.ids() != null ? request.ids() : List.of());
        uniqueIds.remove(null);
        return userRepository.findActiveByIds(uniqueIds)
                             .stream()
                             .map(PublicAuthorResponse::load)
                             .toList();
    }
}
