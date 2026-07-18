package dev.vepo.passport.directory;

import java.util.List;

public record DirectoryPageResponse(List<DirectoryUserResponse> items, int page, int size, long total) {}
