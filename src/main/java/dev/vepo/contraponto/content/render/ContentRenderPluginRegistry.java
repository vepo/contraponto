package dev.vepo.contraponto.content.render;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ContentRenderPluginRegistry {

    private Map<String, ContentRenderPlugin> plugins = Map.of();

    public ContentRenderPlugin find(String identifier) {
        return plugins.get(identifier);
    }

    @PostConstruct
    void loadPlugins() {
        Map<String, ContentRenderPlugin> loaded = new HashMap<>();
        ServiceLoader.load(ContentRenderPlugin.class).forEach(plugin -> {
            String id = plugin.identifier();
            if (id == null || id.isBlank()) {
                throw new IllegalStateException("Content render plugin missing identifier: " + plugin.getClass().getName());
            }
            if (loaded.putIfAbsent(id, plugin) != null) {
                throw new IllegalStateException("Duplicate content render plugin identifier: " + id);
            }
        });
        plugins = Map.copyOf(loaded);
    }
}
