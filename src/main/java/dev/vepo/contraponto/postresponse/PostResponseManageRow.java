package dev.vepo.contraponto.postresponse;

import java.time.LocalDateTime;

public record PostResponseManageRow(long responseId,
                                    long sourcePostId,
                                    String sourcePostTitle,
                                    long responsePostId,
                                    String responsePostTitle,
                                    String responderName,
                                    String responseBlogName,
                                    PostResponseLinkBackStatus linkBackStatus,
                                    LocalDateTime createdAt) {}
