package dev.vepo.passport.directory;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LookupAuthorsRequest(@NotNull @Size(max = 200) List<Long> ids) {}
