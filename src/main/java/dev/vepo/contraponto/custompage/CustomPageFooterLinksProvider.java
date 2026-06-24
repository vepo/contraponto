package dev.vepo.contraponto.custompage;

import dev.vepo.contraponto.shared.infra.FooterLinksProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CustomPageFooterLinksProvider implements FooterLinksProvider {

    private final CustomPageRepository customPageRepository;

    @Inject
    public CustomPageFooterLinksProvider(CustomPageRepository customPageRepository) {
        this.customPageRepository = customPageRepository;
    }

    @Override
    public Links loadGlobalLinks() {
        return customPageRepository.loadLinks();
    }
}
