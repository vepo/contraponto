package dev.vepo.contraponto.content.render;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ContentRenderTagProcessor {

    /** Possessive quantifiers avoid catastrophic backtracking on malformed tags. */
    private static final Pattern RENDER_TAG =
            Pattern.compile("\\{%\\s*+([a-zA-Z][a-zA-Z0-9]*)\\s++([^%]++)\\s*+%\\}");

    private final ContentRenderPluginRegistry registry;

    @Inject
    public ContentRenderTagProcessor(ContentRenderPluginRegistry registry) {
        this.registry = registry;
    }

    public String apply(String content) {
        if (content == null || content.isEmpty()) {
            return content == null ? "" : content;
        }
        Matcher matcher = RENDER_TAG.matcher(content);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String replacement = resolve(matcher.group(1), matcher.group(2));
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private String resolve(String identifier, String paramsGroup) {
        ContentRenderPlugin plugin = registry.find(identifier);
        if (plugin == null) {
            return "{%% %s %s %%}".formatted(identifier, paramsGroup.trim());
        }
        List<String> params = Arrays.asList(paramsGroup.trim().split("\\s+"));
        String html = plugin.render(params);
        if (html == null || html.isBlank()) {
            return "{%% %s %s %%}".formatted(identifier, paramsGroup.trim());
        }
        return html;
    }
}
