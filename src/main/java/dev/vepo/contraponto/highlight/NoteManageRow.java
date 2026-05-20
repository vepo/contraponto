package dev.vepo.contraponto.highlight;

import java.time.LocalDateTime;

public record NoteManageRow(long noteId,
                            long postId,
                            String postTitle,
                            String passageExcerpt,
                            String noteBody,
                            String readerName,
                            LocalDateTime createdAt) {}
