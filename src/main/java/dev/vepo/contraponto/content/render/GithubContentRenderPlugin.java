package dev.vepo.contraponto.content.render;

import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

public final class GithubContentRenderPlugin implements ContentRenderPlugin {

    private static final Pattern REPO_PATH = Pattern.compile("^/([a-zA-Z0-9_.-]+)/([a-zA-Z0-9_.-]+)/?$");

    private static String error(String message) {
        return "<p class=\"content-render content-render--error\">" + escape(message) + "</p>";
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
    }

    @Override
    public String identifier() {
        return "github";
    }

    @Override
    public String render(List<String> params) {
        if (params == null || params.isEmpty()) {
            return error("GitHub repository URL required.");
        }
        String url = String.join(" ", params).trim();
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException ex) {
            return error("Invalid GitHub URL.");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !"github.com".equalsIgnoreCase(uri.getHost())) {
            return error("GitHub URL must be https://github.com/{owner}/{repo}.");
        }
        var matcher = REPO_PATH.matcher(uri.getPath());
        if (!matcher.matches()) {
            return error("Invalid GitHub repository URL.");
        }
        String owner = matcher.group(1);
        String repo = matcher.group(2);
        return """
               <div class="content-render content-render--github">
               <h2 class="github-repo">
               <img class="github-repo__logo" src="/images/plugins/github-logo.png" alt="GitHub" />
               <img class="github-repo__avatar" src="https://github.com/%1$s.png" alt="" />
               <span class="github-repo__path">
               <a href="https://github.com/%1$s" target="_blank" rel="noopener noreferrer">%1$s</a>
               /
               <a href="https://github.com/%1$s/%2$s" target="_blank" rel="noopener noreferrer">%2$s</a>
               </span>
               </h2>
               </div>
               """.formatted(escape(owner), escape(repo));
    }
}
