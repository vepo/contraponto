package dev.vepo.contraponto.highlight;

import java.util.List;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.postresponse.PostResponseCardView;

public record HighlightsSectionView(Post post,
                                    String highlightsUrl,
                                    boolean authenticated,
                                    Long currentUserId,
                                    List<HighlightMarkView> marks,
                                    List<OfficialHighlightView> officialHighlights,
                                    List<PostResponseCardView> approvedResponses,
                                    Long respondsToPostId,
                                    String respondsToTitle,
                                    String respondsToUrl,
                                    String highlightsJson) {}
