package dev.vepo.contraponto.image;

import java.util.ArrayList;
import java.util.List;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.custompage.CustomPagePaths;
import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.post.PostEndpoint;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ImageControlService {

    private final ImageRepository imageRepository;
    private final ImageDependencyRepository dependencyRepository;
    private final PostRepository postRepository;
    private final CustomPageRepository customPageRepository;

    @Inject
    public ImageControlService(ImageRepository imageRepository,
                               ImageDependencyRepository dependencyRepository,
                               PostRepository postRepository,
                               CustomPageRepository customPageRepository) {
        this.imageRepository = imageRepository;
        this.dependencyRepository = dependencyRepository;
        this.postRepository = postRepository;
        this.customPageRepository = customPageRepository;
    }

    public Page<ImageControlRow> listForBlog(Blog blog, PageQuery query) {
        Page<Image> page = imageRepository.findPageByBlogId(blog.getId(), query);
        List<ImageControlRow> rows = page.data().stream().map(img -> toRow(img, blog.getId())).toList();
        return new Page<>(rows, page.page(), page.limit(), page.total());
    }

    private ImageControlRow toRow(Image image, long blogId) {
        List<ImageUsageView> usages = new ArrayList<>();
        for (ImageUsageRow usage : dependencyRepository.findUsagesForImage(image.getId(), blogId)) {
            usages.add(toUsageView(usage));
        }
        if (image.getAltText() == null) {
            return new ImageControlRow(image.getUuid(),
                                       image.getUrl(),
                                       image.getFilename(),
                                       "",
                                       image.getCreatedAt(),
                                       usages);
        }
        return new ImageControlRow(image.getUuid(),
                                   image.getUrl(),
                                   image.getFilename(),
                                   image.getAltText(),
                                   image.getCreatedAt(),
                                   usages);
    }

    private ImageUsageView toUsageView(ImageUsageRow usage) {
        String roleLabel = usage.role() == ImageRole.COVER ? "Cover" : "Inline";
        if (usage.kind() == ImageUsageKind.POST) {
            return postRepository.findById(usage.resourceId())
                                 .map(post -> new ImageUsageView(usage.title(),
                                                                 PostEndpoint.extractUrl(post),
                                                                 roleLabel))
                                 .orElse(new ImageUsageView(usage.title(), "#", roleLabel));
        }
        return customPageRepository.findByIdForManagement(usage.resourceId())
                                   .map(page -> new ImageUsageView(usage.title(),
                                                                   CustomPagePaths.publicUrl(page),
                                                                   roleLabel))
                                   .orElse(new ImageUsageView(usage.title(), "#", roleLabel));
    }

    @Transactional
    public void updateAltText(Blog blog, String uuid, String altText) {
        Image image = imageRepository.findByUuidAndBlogId(uuid, blog.getId())
                                     .orElseThrow(() -> new IllegalArgumentException("Image not found"));
        String trimmed = altText == null ? null : altText.trim();
        image.setAltText(trimmed == null || trimmed.isEmpty() ? null : trimmed);
        imageRepository.update(image);
    }
}
