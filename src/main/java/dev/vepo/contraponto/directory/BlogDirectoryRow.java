package dev.vepo.contraponto.directory;

import java.util.List;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.tag.TagUsage;

public record BlogDirectoryRow(Blog blog, long postCount, String excerpt, List<TagUsage> topTags) {}
