package dev.vepo.passport.profile.create;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateProfileRequest(@NotBlank(message = "Profile name cannot be blank") @Size(min = 3, max = 100, message = "Profile name must be between 3 and 100 characters") String name,
                                   @NotEmpty(message = "At least one role must be associated with the profile") Set<Long> roleIds) {}