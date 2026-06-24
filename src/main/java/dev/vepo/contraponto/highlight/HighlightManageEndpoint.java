package dev.vepo.contraponto.highlight;

import dev.vepo.contraponto.postresponse.PostResponseManageRow;
import dev.vepo.contraponto.postresponse.PostResponseRepository;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Logged
@ApplicationScoped
public class HighlightManageEndpoint {

    @CheckedTemplate
    public static class Templates {
        static native TemplateInstance panel(Page<ProposalManageRow> proposals,
                                             Page<NoteManageRow> notes,
                                             Page<PostResponseManageRow> responses,
                                             String basePath,
                                             String activeTab);

        private Templates() {
            throw new UnsupportedOperationException("Utility class");
        }
    }

    private final CommonHighlightProposalRepository proposalRepository;
    private final HighlightNoteRepository noteRepository;
    private final PostResponseRepository postResponseRepository;
    private final LoggedUser loggedUser;

    @Inject
    public HighlightManageEndpoint(CommonHighlightProposalRepository proposalRepository,
                                   HighlightNoteRepository noteRepository,
                                   PostResponseRepository postResponseRepository,
                                   LoggedUser loggedUser) {
        this.proposalRepository = proposalRepository;
        this.noteRepository = noteRepository;
        this.postResponseRepository = postResponseRepository;
        this.loggedUser = loggedUser;
    }

    public TemplateInstance renderHubPanel(int page, String basePath, String activeTab) {
        PageQuery query = PageQuery.forGrid(20, page);
        var proposals = proposalRepository.findPendingForPostAuthor(loggedUser.getId(), query);
        var notes = noteRepository.findPendingPublicForPostAuthor(loggedUser.getId(), query);
        var responses = postResponseRepository.findForPostAuthor(loggedUser.getId(), query);
        String tab = activeTab != null ? activeTab : "proposals";
        return Templates.panel(proposals, notes, responses, basePath, tab);
    }
}
