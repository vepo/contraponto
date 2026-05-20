package dev.vepo.contraponto.highlight;

import java.util.List;

public record OfficialHighlightView(String anchorClusterHash,
                                    String passage,
                                    String anchorJson,
                                    List<PublicNoteView> approvedNotes) {}
