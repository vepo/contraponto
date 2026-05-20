package dev.vepo.contraponto.highlight;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.vepo.contraponto.notification.NotificationService;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostPublication;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.postresponse.PostResponseCardView;
import dev.vepo.contraponto.postresponse.PostResponseRepository;
import dev.vepo.contraponto.post.PostEndpoint;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class PostTextHighlightService {

    private final PostTextHighlightRepository highlightRepository;
    private final OfficialHighlightRepository officialHighlightRepository;
    private final CommonHighlightProposalRepository proposalRepository;
    private final HighlightNoteRepository noteRepository;
    private final PostResponseRepository postResponseRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final HighlightAnchorClusterer clusterer;
    private final NotificationService notificationService;
    private final HighlightsJsonBuilder highlightsJsonBuilder;
    private final int commonThreshold;
    private final int maxPerUserPerPost;
    private final int maxPassageLength;

    @Inject
    public PostTextHighlightService(PostTextHighlightRepository highlightRepository,
                                    OfficialHighlightRepository officialHighlightRepository,
                                    CommonHighlightProposalRepository proposalRepository,
                                    HighlightNoteRepository noteRepository,
                                    PostResponseRepository postResponseRepository,
                                    PostRepository postRepository,
                                    UserRepository userRepository,
                                    HighlightAnchorClusterer clusterer,
                                    NotificationService notificationService,
                                    HighlightsJsonBuilder highlightsJsonBuilder,
                                    @ConfigProperty(name = "contraponto.highlight.common-threshold", defaultValue = "3") int commonThreshold,
                                    @ConfigProperty(name = "contraponto.highlight.max-per-user-per-post", defaultValue = "20") int maxPerUserPerPost,
                                    @ConfigProperty(name = "contraponto.highlight.max-passage-length", defaultValue = "500") int maxPassageLength) {
        this.highlightRepository = highlightRepository;
        this.officialHighlightRepository = officialHighlightRepository;
        this.proposalRepository = proposalRepository;
        this.noteRepository = noteRepository;
        this.postResponseRepository = postResponseRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.clusterer = clusterer;
        this.notificationService = notificationService;
        this.highlightsJsonBuilder = highlightsJsonBuilder;
        this.commonThreshold = commonThreshold;
        this.maxPerUserPerPost = maxPerUserPerPost;
        this.maxPassageLength = maxPassageLength;
    }

    @Transactional
    public OfficialHighlight approveProposal(long postId, long proposalId, long ownerUserId) {
        CommonHighlightProposal proposal = loadProposalForModeration(postId, proposalId, ownerUserId);
        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new BadRequestException("Proposal is not pending.");
        }
        if (officialHighlightRepository.findByPostAndCluster(postId, proposal.getAnchorClusterHash()).isPresent()) {
            proposal.setStatus(ProposalStatus.APPROVED);
            proposal.setResolvedAt(LocalDateTime.now());
            proposalRepository.save(proposal);
            return officialHighlightRepository.findByPostAndCluster(postId, proposal.getAnchorClusterHash())
                                              .orElseThrow(NotFoundException::new);
        }

        PostTextHighlight canonical = highlightRepository
                                                         .findCanonicalInCluster(postId, proposal.getAnchorClusterHash())
                                                         .orElseThrow(() -> new NotFoundException("No highlight found for this cluster."));

        User owner = userRepository.findById(ownerUserId).orElseThrow(NotFoundException::new);
        OfficialHighlight official = new OfficialHighlight();
        official.setPost(proposal.getPost());
        official.setPublication(canonical.getPublication());
        official.setAnchorClusterHash(proposal.getAnchorClusterHash());
        official.setPassage(canonical.getPassage());
        official.setAnchorJson(canonical.getAnchorJson());
        official.setApprovedBy(owner);
        official.setNeedsReview(false);
        officialHighlightRepository.save(official);

        proposal.setStatus(ProposalStatus.APPROVED);
        proposal.setResolvedAt(LocalDateTime.now());
        proposalRepository.save(proposal);
        return official;
    }

    public HighlightsSectionView buildSectionView(Post post, Long currentUserId) {
        String highlightsUrl = PostEndpoint.extractUrl(post) + "/components/highlights";
        List<OfficialHighlight> officials = officialHighlightRepository.findVisibleForPost(post.getId());
        Set<String> officialClusters = new HashSet<>();
        List<OfficialHighlightView> officialViews = new ArrayList<>();
        for (OfficialHighlight official : officials) {
            officialClusters.add(official.getAnchorClusterHash());
            List<PublicNoteView> notes = noteRepository
                                                       .findApprovedPublicForCluster(post.getId(), official.getAnchorClusterHash())
                                                       .stream()
                                                       .map(n -> new PublicNoteView(n.getUser().getName(), n.getBody()))
                                                       .toList();
            officialViews.add(new OfficialHighlightView(official.getAnchorClusterHash(),
                                                        official.getPassage(),
                                                        official.getAnchorJson(),
                                                        notes));
        }

        List<HighlightMarkView> marks = new ArrayList<>();
        if (currentUserId != null) {
            for (PostTextHighlight h : highlightRepository.findByPostForUser(post.getId(), currentUserId)) {
                boolean isOfficial = officialClusters.contains(h.getAnchorClusterHash());
                marks.add(new HighlightMarkView(h.getId(),
                                                h.getPassage(),
                                                h.getAnchorJson(),
                                                h.getAnchorClusterHash(),
                                                !isOfficial,
                                                isOfficial,
                                                true));
            }
        }

        List<PostResponseCardView> responses = postResponseRepository.findApprovedForSourcePost(post.getId())
                                                                     .stream()
                                                                     .map(r -> {
                                                                         Post rp = r.getResponsePost();
                                                                         String excerpt = rp.getDescription() != null
                                                                                                                      ? rp.getDescription()
                                                                                                                      : "";
                                                                         return new PostResponseCardView(
                                                                                                         rp.getTitle(),
                                                                                                         r.getResponder().getName(),
                                                                                                         excerpt,
                                                                                                         PostEndpoint.extractUrl(rp));
                                                                     })
                                                                     .toList();

        var sectionWithoutJson = new HighlightsSectionView(post,
                                                           highlightsUrl,
                                                           currentUserId != null,
                                                           currentUserId,
                                                           marks,
                                                           officialViews,
                                                           responses,
                                                           null,
                                                           null,
                                                           null,
                                                           "");
        String json = highlightsJsonBuilder.build(sectionWithoutJson);
        return new HighlightsSectionView(post,
                                         highlightsUrl,
                                         currentUserId != null,
                                         currentUserId,
                                         marks,
                                         officialViews,
                                         responses,
                                         null,
                                         null,
                                         null,
                                         json);
    }

    @Transactional
    public PostTextHighlight create(long postId, long userId, String passage, String anchorJson) {
        Post post = loadPublishedPost(postId);
        if (post.getAuthor().getId().equals(userId)) {
            throw new BadRequestException("Authors cannot highlight their own posts.");
        }
        User user = userRepository.findById(userId).orElseThrow(NotFoundException::new);
        String trimmedPassage = validatePassage(passage);
        HighlightAnchor anchor = HighlightAnchor.parse(anchorJson);
        String clusterHash = clusterer.clusterHash(trimmedPassage);

        if (highlightRepository.findByUserPostAndCluster(userId, postId, clusterHash).isPresent()) {
            throw new BadRequestException("You already highlighted this passage.");
        }
        if (highlightRepository.countByUserAndPost(userId, postId) >= maxPerUserPerPost) {
            throw new BadRequestException("Maximum highlights per post reached.");
        }

        PostPublication publication = post.getLivePublication();
        if (publication == null) {
            throw new NotFoundException("Post has no live publication.");
        }

        PostTextHighlight highlight = new PostTextHighlight();
        highlight.setPost(post);
        highlight.setPublication(publication);
        highlight.setUser(user);
        highlight.setPassage(trimmedPassage);
        highlight.setAnchorJson(anchor.toJson());
        highlight.setAnchorClusterHash(clusterHash);
        highlightRepository.save(highlight);

        maybeCreateOrRefreshProposal(post, clusterHash, trimmedPassage);
        return highlight;
    }

    private CommonHighlightProposal loadProposalForModeration(long postId, long proposalId, long ownerUserId) {
        CommonHighlightProposal proposal = proposalRepository.findById(proposalId)
                                                             .orElseThrow(NotFoundException::new);
        if (proposal.getPost().getId() != postId) {
            throw new NotFoundException("Proposal not found.");
        }
        if (!proposal.getPost().getAuthor().getId().equals(ownerUserId)) {
            throw new ForbiddenException("Only the post owner can moderate highlights.");
        }
        return proposal;
    }

    private Post loadPublishedPost(long postId) {
        Post post = postRepository.findById(postId).orElseThrow(NotFoundException::new);
        if (!post.isPublished() || post.getLivePublication() == null) {
            throw new NotFoundException("Post not found.");
        }
        return post;
    }

    private void maybeCreateOrRefreshProposal(Post post, String clusterHash, String passage) {
        if (officialHighlightRepository.findByPostAndCluster(post.getId(), clusterHash).isPresent()) {
            return;
        }
        long readerCount = highlightRepository.countDistinctReadersInCluster(post.getId(), clusterHash);
        if (readerCount < commonThreshold) {
            return;
        }

        var existing = proposalRepository.findByPostAndCluster(post.getId(), clusterHash);
        if (existing.isPresent()) {
            CommonHighlightProposal proposal = existing.get();
            if (proposal.getStatus() == ProposalStatus.REJECTED) {
                return;
            }
            proposal.setReaderCount((int) readerCount);
            proposal.setPassage(passage);
            if (proposal.getStatus() == ProposalStatus.PENDING) {
                proposalRepository.save(proposal);
                return;
            }
            return;
        }

        CommonHighlightProposal proposal = new CommonHighlightProposal();
        proposal.setPost(post);
        proposal.setAnchorClusterHash(clusterHash);
        proposal.setPassage(passage);
        proposal.setReaderCount((int) readerCount);
        proposal.setStatus(ProposalStatus.PENDING);
        proposalRepository.save(proposal);
        notificationService.notifyCommonHighlightProposal(post.getAuthor(), post);
    }

    @Transactional
    public void rejectProposal(long postId, long proposalId, long ownerUserId) {
        CommonHighlightProposal proposal = loadProposalForModeration(postId, proposalId, ownerUserId);
        proposal.setStatus(ProposalStatus.REJECTED);
        proposal.setResolvedAt(LocalDateTime.now());
        proposalRepository.save(proposal);
    }

    @Transactional
    public void remove(long postId, long highlightId, long userId) {
        PostTextHighlight highlight = highlightRepository.findById(highlightId)
                                                         .orElseThrow(NotFoundException::new);
        if (highlight.getPost().getId() != postId) {
            throw new NotFoundException("Highlight not found.");
        }
        if (!highlight.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You can only remove your own highlights.");
        }
        highlightRepository.delete(highlight);
    }

    private String validatePassage(String passage) {
        if (passage == null) {
            throw new BadRequestException("Highlight passage is required.");
        }
        String trimmed = passage.trim();
        if (trimmed.isEmpty()) {
            throw new BadRequestException("Highlight passage is required.");
        }
        if (trimmed.length() > maxPassageLength) {
            throw new BadRequestException("Highlight passage must be at most " + maxPassageLength + " characters.");
        }
        return trimmed;
    }
}
