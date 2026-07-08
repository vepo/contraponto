package dev.vepo.contraponto.activitypub.discovery;

import java.util.List;

public record WebFingerJrd(String subject, List<String> aliases, List<WebFingerLink> links) {}