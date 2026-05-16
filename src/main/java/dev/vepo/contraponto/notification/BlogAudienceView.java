package dev.vepo.contraponto.notification;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record BlogAudienceView(long blogId,
                               boolean showControls,
                               boolean authenticated,
                               boolean following,
                               boolean emailSubscribed) {}
