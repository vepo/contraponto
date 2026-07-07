package dev.vepo.contraponto.activitypub;

import java.util.Map;
import java.util.Optional;

import dev.vepo.contraponto.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

@ApplicationScoped
public class ActivityPubActorService {

    private final ActivityPubSettings settings;
    private final ActivityPubActorRepository actorRepository;
    private final ActivityPubKeyPairService keyPairService;
    private final ActivityPubActorDocumentBuilder documentBuilder;
    private final dev.vepo.contraponto.blog.BlogSubdomainConfig subdomainConfig;

    @Inject
    public ActivityPubActorService(ActivityPubSettings settings,
                                   ActivityPubActorRepository actorRepository,
                                   ActivityPubKeyPairService keyPairService,
                                   ActivityPubActorDocumentBuilder documentBuilder,
                                   dev.vepo.contraponto.blog.BlogSubdomainConfig subdomainConfig) {
        this.settings = settings;
        this.actorRepository = actorRepository;
        this.keyPairService = keyPairService;
        this.documentBuilder = documentBuilder;
        this.subdomainConfig = subdomainConfig;
    }

    public Map<String, Object> buildActorDocument(User user, ActivityPubActor actor) {
        return documentBuilder.buildPerson(user, actor);
    }

    public ActivityPubActor disableFederation(User user) {
        var actor = actorRepository.findByUserId(user.getId())
                                   .orElseThrow(() -> new BadRequestException("ActivityPub actor not found"));
        actor.disableFederation();
        return actorRepository.update(actor);
    }

    public ActivityPubActor enableFederation(User user) {
        if (!settings.enabled()) {
            throw new BadRequestException("ActivityPub federation is disabled on this instance");
        }
        var existing = actorRepository.findByUserId(user.getId());
        if (existing.isPresent()) {
            var actor = existing.get();
            actor.enableFederation();
            return actorRepository.update(actor);
        }
        var keyPair = keyPairService.generateRsaKeyPair();
        var publicKeyPem = keyPairService.toPublicKeyPem(keyPair.getPublic());
        var publicKeyId = ActivityPubPaths.publicKeyId(user, subdomainConfig);
        var actor = new ActivityPubActor(user,
                                         true,
                                         keyPairService.encryptPrivateKey(keyPair.getPrivate()),
                                         publicKeyPem,
                                         publicKeyId);
        return actorRepository.create(actor);
    }

    public Optional<ActivityPubActor> findByUserId(long userId) {
        return actorRepository.findByUserId(userId);
    }

    public Optional<ActivityPubActor> findEnabledByUsername(String username) {
        if (!settings.enabled()) {
            return Optional.empty();
        }
        return actorRepository.findEnabledByUsername(username);
    }
}
