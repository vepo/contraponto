package dev.vepo.contraponto.image;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ContentImageMarkerService {

    static final String MARKER_PREFIX = "<!-- contraponto:image uuid=\"";
    static final String MARKER_SUFFIX = "\" -->";

    private static final Pattern MARKER_LINE = Pattern.compile("^\\s*<!-- contraponto:image uuid=\"([0-9a-fA-F\\-]{36})\" -->\\s*$");
    public static final Pattern IMAGE_URL = Pattern.compile("/api/images/([0-9a-fA-F\\-]{36})\\.[a-zA-Z0-9]+");

    public Set<String> extractImageUuids(String content) {
        var uuids = new LinkedHashSet<String>();
        if (content == null || content.isBlank()) {
            return uuids;
        }
        for (String line : content.split("\n")) {
            Matcher m = MARKER_LINE.matcher(line);
            if (m.matches()) {
                uuids.add(m.group(1));
            }
        }
        uuids.addAll(extractUuidsFromUrls(content));
        return uuids;
    }

    Set<String> extractUuidsFromUrls(String content) {
        var uuids = new LinkedHashSet<String>();
        Matcher matcher = IMAGE_URL.matcher(content);
        while (matcher.find()) {
            uuids.add(matcher.group(1));
        }
        return uuids;
    }

    public String markerLine(String uuid) {
        return MARKER_PREFIX + uuid + MARKER_SUFFIX;
    }

    public String stripMarkersForExport(String content) {
        return toEditorContent(content);
    }

    public String toEditorContent(String stored) {
        if (stored == null || stored.isBlank()) {
            return stored == null ? "" : stored;
        }
        String[] lines = stored.split("\n", -1);
        var out = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (MARKER_LINE.matcher(lines[i]).matches()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append('\n');
            }
            out.append(lines[i]);
        }
        return out.toString();
    }

    public String toStoredContent(String editorContent) {
        if (editorContent == null) {
            return "";
        }
        Set<String> uuidsInContent = extractUuidsFromUrls(editorContent);
        String[] lines = editorContent.split("\n", -1);
        var result = new ArrayList<String>();
        for (String line : lines) {
            if (MARKER_LINE.matcher(line).matches()) {
                continue;
            }
            Matcher urlMatcher = IMAGE_URL.matcher(line);
            if (urlMatcher.find()) {
                String uuid = urlMatcher.group(1);
                if (uuidsInContent.contains(uuid)) {
                    String marker = markerLine(uuid);
                    if (result.isEmpty() || !marker.equals(result.get(result.size() - 1))) {
                        result.add(marker);
                    }
                }
            }
            result.add(line);
        }
        return String.join("\n", result);
    }
}
