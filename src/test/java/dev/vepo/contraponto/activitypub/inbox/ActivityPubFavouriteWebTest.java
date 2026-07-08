package dev.vepo.contraponto.activitypub.inbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.activitypub.remote.ActivityPubRemoteActor;
import dev.vepo.contraponto.activitypub.remote.ActivityPubRemoteActorRepository;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebReaderTest;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;

@WebReaderTest
class ActivityPubFavouriteWebTest {

    @Inject
    ActivityPubFavouriteRepository favouriteRepository;

    @Inject
    ActivityPubRemoteActorRepository remoteActorRepository;

    private User author;
    private Post post;

    @Test
    void authorSeesFediverseFavouriteList(App app) {
        app.login(author)
           .goTo(post);
        app.assertPageSourceContains("Fediverse favourites");
        app.assertPageSourceContains("@liker@remote.example");
        app.assertPageSourceContains("@reader@remote.example");
    }

    @Test
    void guestSeesFediverseFavouriteCount(App app) {
        app.access()
           .goTo(post);
        app.assertPageSourceContains("2 Fediverse favourites");
        app.assertPageSourceDoesNotContain("@liker@remote.example");
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("favauthor")
                      .withEmail("favauthor@test.com")
                      .withName("Fav Author")
                      .withPassword("password123")
                      .persist();
        Given.activityPubActor().withUser(author).persist();
        post = Given.post()
                    .withAuthor(author)
                    .withTitle("Federated Favourites Post")
                    .withSlug("federated-favourites-post")
                    .withContent("Post body for Fediverse favourite display.")
                    .persist();
        Given.transaction(() -> {
            var liker = new ActivityPubRemoteActor("https://remote.example/users/liker", "https://remote.example/inbox");
            liker.applyFetchedProfile("https://remote.example/inbox", null, null, "Liker", "liker");
            remoteActorRepository.create(liker);
            var reader = new ActivityPubRemoteActor("https://remote.example/users/reader", "https://remote.example/inbox");
            reader.applyFetchedProfile("https://remote.example/inbox", null, null, "Reader", "reader");
            remoteActorRepository.create(reader);
            favouriteRepository.create(new ActivityPubFavourite(post, liker, "https://remote.example/like/1"));
            favouriteRepository.create(new ActivityPubFavourite(post, reader, "https://remote.example/like/2"));
        });
    }
}
