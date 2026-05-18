package dev.vepo.contraponto.image;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ImageAltEnricher {

    private static final Pattern IMG_TAG = Pattern.compile(
                                                           "(<img\\s[^>]*?\\bsrc\\s*=\\s*[\"'])(/api/images/([0-9a-fA-F-]{36})\\.[a-zA-Z0-9]+)([\"'][^>]*>)",
                                                           Pattern.CASE_INSENSITIVE);

    private static String escapeAttr(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;");
    }

    private final ImageRepository imageRepository;

    private final ContentImageMarkerService markerService;

    @Inject
    public ImageAltEnricher(ImageRepository imageRepository, ContentImageMarkerService markerService) {
        this.imageRepository = imageRepository;
        this.markerService = markerService;
    }

    public String enrichHtml(String html) {
        if (html == null || html.isBlank()) {
            return html == null ? "" : html;
        }
        Set<String> uuids = markerService.extractUuidsFromUrls(html);
        if (uuids.isEmpty()) {
            return html;
        }
        Map<String, String> alts = new HashMap<>();
        for (String uuid : uuids) {
            imageRepository.findByUuid(uuid).ifPresent(img -> {
                if (img.getAltText() != null && !img.getAltText().isBlank()) {
                    alts.put(uuid, escapeAttr(img.getAltText()));
                }
            });
        }
        if (alts.isEmpty()) {
            return html;
        }
        Matcher matcher = IMG_TAG.matcher(html);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String uuid = matcher.group(3);
            String alt = alts.get(uuid);
            String replacement = matcher.group(1) + matcher.group(2) + matcher.group(4);
            if (alt != null) {
                String tag = matcher.group(0);
                if (tag.matches("(?is).*\\balt\\s*=.*")) {
                    replacement = tag.replaceAll("(?is)\\balt\\s*=\\s*[\"'][^\"']*[\"']", "alt=\"" + alt + "\"");
                } else {
                    replacement = tag.replaceFirst(">$", " alt=\"" + alt + "\">");
                }
            } else {
                replacement = matcher.group(0);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
