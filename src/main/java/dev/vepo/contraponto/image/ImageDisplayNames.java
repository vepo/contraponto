package dev.vepo.contraponto.image;

final class ImageDisplayNames {

    static String displayFilename(Image image) {
        String gitPath = image.getGitAssetRelativePath();
        if (gitPath != null && !gitPath.isBlank()) {
            int dot = image.getUrl().lastIndexOf('.');
            String ext = dot >= 0 ? image.getUrl().substring(dot) : "";
            return gitPath + ext;
        }
        return image.getFilename();
    }

    private ImageDisplayNames() {
        throw new UnsupportedOperationException("Utility class");
    }
}
