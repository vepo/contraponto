package dev.vepo.contraponto.highlight;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostPaths;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.postresponse.PostResponse;
import dev.vepo.contraponto.postresponse.PostResponseService;
import dev.vepo.contraponto.user.LoggedUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@ApplicationScoped
@Path("{username}")
public class HighlightComponentEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance highlights(HighlightsSectionView section);

        private Templates() {
            throw new UnsupportedOperationException("Utility class");
        }
    }

    private final PostRepository postRepository;
    private final PostTextHighlightService highlightService;
    private final PostResponseService postResponseService;
    private final LoggedUser loggedUser;

    @Inject
    public HighlightComponentEndpoint(PostRepository postRepository,
                                      PostTextHighlightService highlightService,
                                      PostResponseService postResponseService,
                                      LoggedUser loggedUser) {
        this.postRepository = postRepository;
        this.highlightService = highlightService;
        this.postResponseService = postResponseService;
        this.loggedUser = loggedUser;
    }

    @GET
    @Path("{blogSlug}/post/{slug}/components/highlights")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance blogHighlights(@PathParam("username") String username,
                                           @PathParam("blogSlug") String blogSlug,
                                           @PathParam("slug") String slug) {
        Post post = postRepository.findBlogPost(username, blogSlug, slug).orElseThrow(NotFoundException::new);
        return renderHighlights(post);
    }

    @GET
    @Path("post/{slug}/components/highlights")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance mainBlogHighlights(@PathParam("username") String username,
                                               @PathParam("slug") String slug) {
        Post post = postRepository.findMainBlogPost(username, slug).orElseThrow(NotFoundException::new);
        return renderHighlights(post);
    }

    public TemplateInstance renderHighlights(Post post) {
        Long userId = loggedUser.isAuthenticated() ? loggedUser.getId() : null;
        HighlightsSectionView section = highlightService.buildSectionView(post, userId);
        PostResponse response = postResponseService.findByResponsePost(post.getId());
        if (response != null) {
            Post source = response.getSourcePost();
            section = new HighlightsSectionView(section.post(),
                                                section.highlightsUrl(),
                                                section.authenticated(),
                                                section.currentUserId(),
                                                section.marks(),
                                                section.officialHighlights(),
                                                section.readerNotes(),
                                                section.approvedResponses(),
                                                source.getId(),
                                                source.getTitle(),
                                                PostPaths.extractUrl(source),
                                                section.highlightsJson());
        }
        return Templates.highlights(section);
    }
}
