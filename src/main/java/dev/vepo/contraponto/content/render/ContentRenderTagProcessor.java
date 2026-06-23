package dev.vepo.contraponto.content.render;

import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ContentRenderTagProcessor {

    /** Possessive quantifiers avoid catastrophic backtracking on malformed tags. */
    private static final Pattern RENDER_TAG =
            Pattern.compile("\\{%\\s*+([a-zA-Z][a-zA-Z0-9]*)\\s++([^%]++)\\s*+%\\}");

    private static String wrapForAsciiDocPassthrough(String html) {
        return "++++\n%s\n++++".formatted(html);
    }

    private final ContentRenderPluginRegistry registry;

    @Inject
    public ContentRenderTagProcessor(ContentRenderPluginRegistry registry) {
        this.registry = registry;
    }

    public String apply(String content) {
        return apply(content, html -> html);
    }

    private String apply(String content, UnaryOperator<String> htmlWrapper) {
        if (content == null || content.isEmpty()) {
            return content == null ? "" : content;
        }
        Matcher matcher = RENDER_TAG.matcher(content);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String replacement = resolve(matcher.group(1), matcher.group(2), htmlWrapper);
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /**
     * Expands render tags and wraps each plugin's HTML in an AsciiDoc passthrough
     * block so Asciidoctor safe mode preserves embed markup and does not autolink
     * URLs inside tags.
     */
    public String applyWithAsciiDocPassthrough(String content) {
        return apply(content, ContentRenderTagProcessor::wrapForAsciiDocPassthrough);
    }

    private String resolve(String identifier, String paramsGroup, UnaryOperator<String> htmlWrapper) {
        ContentRenderPlugin plugin = registry.find(identifier);
        if (plugin == null) {
            return "{%% %s %s %%}".formatted(identifier, paramsGroup.trim());
        }
        List<String> params = Arrays.asList(paramsGroup.trim().split("\\s+"));
        String html = plugin.render(params);
        if (html == null || html.isBlank()) {
            return "{%% %s %s %%}".formatted(identifier, paramsGroup.trim());
        }
        return htmlWrapper.apply(html);
    }
}
