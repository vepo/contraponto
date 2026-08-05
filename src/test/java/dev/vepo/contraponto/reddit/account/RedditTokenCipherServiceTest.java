package dev.vepo.contraponto.reddit.account;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class RedditTokenCipherServiceTest {

    @Inject
    RedditTokenCipherService cipherService;

    @Test
    void ciphertextDoesNotContainPlaintext() {
        var refreshToken = "another-refresh-token";
        var encrypted = cipherService.encrypt(refreshToken);
        assertThat(encrypted).doesNotContain(refreshToken);
    }

    @Test
    void encryptDecryptRoundTripReturnsOriginalToken() {
        var refreshToken = "reddit-refresh-token-1234567890";

        var encrypted = cipherService.encrypt(refreshToken);
        var decrypted = cipherService.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(refreshToken);
    }

    @Test
    void successiveEncryptionsOfSameTokenDiffer() {
        var refreshToken = "same-token-every-time";
        var first = cipherService.encrypt(refreshToken);
        var second = cipherService.encrypt(refreshToken);

        assertThat(first).isNotEqualTo(second);
        assertThat(cipherService.decrypt(first)).isEqualTo(refreshToken);
        assertThat(cipherService.decrypt(second)).isEqualTo(refreshToken);
    }
}
