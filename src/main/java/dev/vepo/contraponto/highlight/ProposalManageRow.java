package dev.vepo.contraponto.highlight;

import java.time.LocalDateTime;

public record ProposalManageRow(long proposalId,
                                long postId,
                                String postTitle,
                                String passage,
                                int readerCount,
                                LocalDateTime createdAt) {}
