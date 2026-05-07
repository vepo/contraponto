package dev.vepo.contraponto.image;

public record ImageData(byte[] data, String contentType, long size) {}