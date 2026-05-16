package dev.vepo.contraponto.notification;

import dev.vepo.contraponto.blog.Blog;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record SubscriptionRow(Blog blog, BlogAudienceView audience) {}
