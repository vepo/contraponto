package dev.vepo.contraponto.image;

public record ImageResponse(String id, String url, String filename, String contentType, Long size, String altText) {}
