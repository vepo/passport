package dev.vepo.passport.profile.create;

import java.util.Set;

import org.jboss.resteasy.reactive.ResponseStatus;

import dev.vepo.passport.model.Profile;
import dev.vepo.passport.model.Role;
import dev.vepo.passport.profile.ProfileRepository;
import dev.vepo.passport.profile.ProfileResponse;
import dev.vepo.passport.role.RoleRepository;
import dev.vepo.passport.shared.security.RequiredRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

@Path("/profiles")
@ApplicationScoped
@RolesAllowed(RequiredRoles.ADMIN)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CreateProfileEndpoint {
    private final ProfileRepository profileRepository;
    private final RoleRepository roleRepository;

    @Inject
    public CreateProfileEndpoint(ProfileRepository profileRepository,
                                 RoleRepository roleRepository) {
        this.profileRepository = profileRepository;
        this.roleRepository = roleRepository;
    }

    @POST
    @Transactional
    @ResponseStatus(201)
    public ProfileResponse create(@Valid CreateProfileRequest request) {
        // Check for existing profile with same name
        profileRepository.findByName(request.name())
                         .ifPresent(existingProfile -> {
                             throw new WebApplicationException("Profile with name '%s' already exists".formatted(request.name()),
                                                               Status.CONFLICT);
                         });

        // Load all requested roles
        Set<Role> roles = loadRoles(request.roleIds());

        // Create and save the profile
        Profile profile = profileRepository.save(new Profile(request.name(), roles));

        return ProfileResponse.from(profile);
    }

    private Set<Role> loadRoles(Set<Long> roleIds) {
        Set<Role> roles = roleRepository.findByIds(roleIds);
        if (roles.size() != roleIds.size()) {
            throwRoleNotFoundException(roleIds, roles);
        }
        return roles;
    }

    private void throwRoleNotFoundException(Set<Long> requestedRoleIds, Set<Role> foundRoles) {
        var foundRoleIds = foundRoles.stream()
                                     .map(Role::getId)
                                     .toList();
        var notFoundRoleIds = requestedRoleIds.stream()
                                              .filter(id -> !foundRoleIds.contains(id))
                                              .toList();
        throw new NotFoundException("Could not find roles! ids=%s".formatted(notFoundRoleIds));
    }
}