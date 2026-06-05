package dev.vepo.contraponto.tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.vepo.contraponto.post.Post;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TagService {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final TagRepository tagRepository;

    @Inject
    public TagService(ObjectMapper objectMapper, TagRepository tagRepository) {
        this.objectMapper = objectMapper;
        this.tagRepository = tagRepository;
    }

    private List<String> parseTagLabels(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return List.of();
        }
        try {
            List<String> raw = objectMapper.readValue(tagsJson, STRING_LIST);
            List<String> out = new ArrayList<>();
            for (String s : raw) {
                if (s != null && !s.isBlank()) {
                    out.add(s.trim());
                }
            }
            return out;
        } catch (Exception _) {
            return List.of();
        }
    }

    public void syncPostTags(Post post, String tagsJson) {
        List<String> labels = parseTagLabels(tagsJson);
        post.getTags().clear();
        if (labels.isEmpty()) {
            return;
        }
        LinkedHashMap<String, Tag> bySlug = new LinkedHashMap<>();
        for (String label : labels) {
            if (label == null || label.isBlank()) {
                continue;
            }
            Tag tag = tagRepository.findOrCreateByLabel(label);
            bySlug.put(tag.getSlug(), tag);
        }
        post.getTags().addAll(bySlug.values());
        tagRepository.flush();
    }

    public String tagsToJson(Post post) {
        try {
            List<String> names = post.getTags().stream().map(Tag::getName).sorted(String.CASE_INSENSITIVE_ORDER).toList();
            return objectMapper.writeValueAsString(names);
        } catch (Exception _) {
            return "[]";
        }
    }
}
