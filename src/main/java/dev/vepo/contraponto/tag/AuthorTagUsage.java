package dev.vepo.contraponto.tag;

import dev.vepo.contraponto.user.User;

public record AuthorTagUsage(User author, long postCount) {}
