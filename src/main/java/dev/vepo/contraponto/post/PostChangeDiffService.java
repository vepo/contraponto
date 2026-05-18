package dev.vepo.contraponto.post;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;

import dev.vepo.contraponto.shared.infra.TemplateExtensions;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PostChangeDiffService {

    public List<VersionDiff> buildVersionDiffs(List<PostPublication> newestFirst) {
        if (newestFirst == null || newestFirst.isEmpty()) {
            return List.of();
        }
        List<PostPublication> ordered = newestFirst.reversed();

        List<VersionDiff> result = new ArrayList<>();
        PostPublication previous = null;
        for (PostPublication current : ordered) {
            PublicationDiff diff = previous == null ? null : diff(previous, current);
            result.add(new VersionDiff(current, diff));
            previous = current;
        }
        return result.reversed();
    }

    public PublicationDiff diff(PostPublication previous, PostPublication current) {
        boolean titleChanged = !Objects.equals(nullToEmpty(previous.getTitle()), nullToEmpty(current.getTitle()));
        boolean descriptionChanged = !Objects.equals(nullToEmpty(previous.getDescription()),
                                                     nullToEmpty(current.getDescription()));
        String contentDiffHtml = renderContentDiff(previous.getContent(), current.getContent());
        boolean contentChanged = !contentDiffHtml.isBlank();
        return new PublicationDiff(titleChanged,
                                   descriptionChanged,
                                   contentChanged,
                                   contentDiffHtml,
                                   previousTitleLine(previous, current, titleChanged),
                                   descriptionLine(descriptionChanged));
    }

    private static String previousTitleLine(PostPublication previous, PostPublication current, boolean changed) {
        if (!changed) {
            return "";
        }
        return "Title: \"%s\" → \"%s\"".formatted(escape(previous.getTitle()), escape(current.getTitle()));
    }

    private static String descriptionLine(boolean changed) {
        if (!changed) {
            return "";
        }
        return "Description changed";
    }

    public String renderContentDiff(String before, String after) {
        List<String> beforeLines = splitLines(before);
        List<String> afterLines = splitLines(after);
        Patch<String> patch = DiffUtils.diff(beforeLines, afterLines);
        if (patch.getDeltas().isEmpty()) {
            return "";
        }

        StringBuilder html = new StringBuilder();
        html.append("<pre class=\"post-history__diff\">");
        for (AbstractDelta<String> delta : patch.getDeltas()) {
            if (delta.getType() == DeltaType.DELETE || delta.getType() == DeltaType.CHANGE) {
                for (String line : delta.getSource().getLines()) {
                    html.append("<span class=\"post-history__diff-del\">- ")
                        .append(escape(line))
                        .append("</span>\n");
                }
            }
            if (delta.getType() == DeltaType.INSERT || delta.getType() == DeltaType.CHANGE) {
                for (String line : delta.getTarget().getLines()) {
                    html.append("<span class=\"post-history__diff-ins\">+ ")
                        .append(escape(line))
                        .append("</span>\n");
                }
            }
        }
        html.append("</pre>");
        return html.toString();
    }

    private static List<String> splitLines(String text) {
        if (text == null || text.isEmpty()) {
            return List.of("");
        }
        return List.of(text.split("\\R", -1));
    }

    private static String escape(String value) {
        return TemplateExtensions.escapeHtml(value == null ? "" : value);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public record PublicationDiff(boolean titleChanged,
                                  boolean descriptionChanged,
                                  boolean contentChanged,
                                  String contentDiffHtml,
                                  String titleChangeSummary,
                                  String descriptionChangeSummary) {

        public boolean hasChanges() {
            return titleChanged || descriptionChanged || contentChanged;
        }
    }

    public record VersionDiff(PostPublication publication, PublicationDiff diffFromPrevious) {

        public boolean isFirstVersion() {
            return diffFromPrevious == null;
        }
    }
}
