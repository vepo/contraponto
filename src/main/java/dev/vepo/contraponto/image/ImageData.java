package dev.vepo.contraponto.image;

import java.util.Arrays;
import java.util.Objects;

public record ImageData(byte[] data, String contentType, long size) {
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        ImageData other = (ImageData) obj;
        return size == other.size && Objects.equals(contentType, other.contentType) && Arrays.equals(data, other.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(size, contentType, Objects.hashCode(data));
    }

    @Override
    public String toString() {
        return "ImageData[contentType=%s, size=%d, data=%s]".formatted(contentType, size, Arrays.toString(data));
    }
}