package dev.vepo.contraponto.image;

public class ImageResponse {

    private String id;
    private String url;
    private String filename;
    private String contentType;
    private Long size;

    public ImageResponse(String id, String url, String filename, String contentType, Long size) {
        this.id = id;
        this.url = url;
        this.filename = filename;
        this.contentType = contentType;
        this.size = size;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}

record ImageData(byte[] data, String contentType, long size) {}