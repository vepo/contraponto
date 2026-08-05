package dev.vepo.contraponto.reddit.account;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import dev.vepo.contraponto.reddit.RedditSettings;
import dev.vepo.contraponto.user.User;

@ApplicationScoped
public class RedditAppearanceService {

    private final RedditSettings settings;
    private final UserRedditAccountRepository accountRepository;

    @Inject
    public RedditAppearanceService(RedditSettings settings, UserRedditAccountRepository accountRepository) {
        this.settings = settings;
        this.accountRepository = accountRepository;
    }

    public RedditAccountView buildView(User user) {
        var account = accountRepository.findByUserId(user.getId()).orElse(null);
        return new RedditAccountView(settings.enabled(), account != null, account == null ? null : account.getRedditUsername());
    }
}
