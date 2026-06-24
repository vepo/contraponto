package dev.vepo.contraponto.comment;

import java.util.List;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.user.LoggedUser;

public record CommentsSectionView(Post post,
                                  String commentsUrl,
                                  List<CommentView> roots,
                                  List<CommentView> pending,
                                  boolean authenticated,
                                  boolean postOwner,
                                  LoggedUser user) {}
