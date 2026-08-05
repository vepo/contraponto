package dev.vepo.contraponto.git.legacylink;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rewrites only genuine link <em>targets</em> in Markdown/AsciiDoc/HTML bodies
 * — never plain prose that happens to mention a matching URL outside a link —
 * mirroring the structural (link-syntax-aware) approach already used by
 * {@link GitImageSyncService} for asset URLs.
 */
public final class LegacyLinkTargetRewriter {

    private static final Pattern MARKDOWN_LINK = Pattern.compile("(!?\\[[^\\]]*\\]\\()([^)]+)(\\))");
    private static final Pattern AUTOLINK = Pattern.compile("(<)(https?://[^>\\s]+)(>)");
    private static final Pattern HTML_HREF = Pattern.compile("(href=[\"'])([^\"']+)([\"'])", Pattern.CASE_INSENSITIVE);
    private static final Pattern ASCIIDOC_LINK = Pattern.compile("(link:)([^\\[\\s]+)(\\[)");

    public static String rewrite(String body, Function<String, Optional<String>> targetResolver) {
        if (body == null || body.isBlank()) {
            return body;
        }
        String result = rewriteWithPattern(body, MARKDOWN_LINK, targetResolver);
        result = rewriteWithPattern(result, AUTOLINK, targetResolver);
        result = rewriteWithPattern(result, HTML_HREF, targetResolver);
        result = rewriteWithPattern(result, ASCIIDOC_LINK, targetResolver);
        return result;
    }

    private static String rewriteWithPattern(String body, Pattern pattern, Function<String, Optional<String>> targetResolver) {
        Matcher m = pattern.matcher(body);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String url = m.group(2);
            Optional<String> replacement = targetResolver.apply(url);
            if (replacement.isEmpty()) {
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
            } else {
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + replacement.get() + m.group(3)));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private LegacyLinkTargetRewriter() {}
}
