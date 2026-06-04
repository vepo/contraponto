package dev.vepo.contraponto.image;

import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import dev.vepo.contraponto.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ImagePickerService {

    static final int PICKER_PAGE_SIZE = 8;

    private final ImageRepository imageRepository;

    @Inject
    public ImagePickerService(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    public Page<ImagePickerItem> listForOwner(User owner, String searchQuery, PageQuery query) {
        Page<Image> page = imageRepository.findPageByOwnerId(owner.getId(), searchQuery, query);
        var items = page.data().stream().map(this::toItem).toList();
        return new Page<>(items, page.page(), page.limit(), page.total());
    }

    private ImagePickerItem toItem(Image image) {
        String displayFilename = ImageDisplayNames.displayFilename(image);
        String altText = image.getAltText() == null ? "" : image.getAltText();
        return new ImagePickerItem(image.getUuid(),
                                   image.getUrl(),
                                   displayFilename,
                                   altText);
    }
}
