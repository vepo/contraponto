package dev.vepo.contraponto.dashboard;

import java.util.List;
import java.util.Map;

import dev.vepo.contraponto.post.Post;

public record DashboardPage(long draftsCount,
                            long publishedCount,
                            List<Post> recentDrafts,
                            List<Post> recentPublished,
                            Map<Long, Long> viewCounts,
                            Long selectedBlogId) {}
