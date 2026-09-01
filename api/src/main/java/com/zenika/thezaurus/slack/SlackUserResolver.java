package com.zenika.thezaurus.slack;

import com.slack.api.bolt.App;
import com.slack.api.methods.response.users.UsersLookupByEmailResponse;
import com.zenika.thezaurus.repository.UserRepository;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Résout le {@code slackUserId} depuis l'email, pour les personnes que la commande {@code /talk}
 * n'a jamais créées. Best-effort : tout échec n'écrit rien et sera rejoué au login suivant.
 * Cf. {@code docs/adr/0001}.
 */
@ApplicationScoped
public class SlackUserResolver {

    private static final Logger logger = Logger.getLogger(SlackUserResolver.class);

    @Inject
    App slackApp;

    @Inject
    UserRepository userRepository;

    @ConfigProperty(name = "thezaurus.slack.bot-token")
    Optional<String> botToken;

    /**
     * Rend la main immédiatement : le login ne doit pas attendre Slack. Sur le worker pool de
     * Quarkus, pas le {@code ForkJoinPool.commonPool}, qui n'est pas fait pour de l'I/O bloquante.
     */
    public CompletableFuture<Void> resolveAndPersistAsync(String email) {
        return CompletableFuture.runAsync(() -> resolveAndPersist(email), Infrastructure.getDefaultWorkerPool());
    }

    /** Variante synchrone, point d'entrée des tests. Ne lève jamais : tout échec est absorbé. */
    public void resolveAndPersist(String email) {
        try {
            resolveByEmail(email).ifPresent(slackUserId -> {
                try {
                    userRepository.updateSlackUserId(email, slackUserId);
                    logger.infof("Compte Slack %s rattaché à %s", slackUserId, email);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warnf("Rattachement Slack de %s interrompu", email);
                } catch (Exception e) {
                    logger.warnf(e, "Échec de la persistance du rattachement Slack de %s", email);
                }
            });
        } catch (Exception e) {
            // Sans ce filet, l'exception disparaîtrait dans le CompletableFuture.
            logger.warnf(e, "Échec de la résolution du compte Slack de %s", email);
        }
    }

    /** Optional vide dès que le compte n'est pas résolu, sans distinguer les causes d'échec. */
    private Optional<String> resolveByEmail(String email) {
        // Sans bot-token, l'App Bolt est produite sans token : l'appel serait voué à l'échec.
        Optional<String> token = botToken.filter(Predicate.not(String::isBlank));
        if (token.isEmpty()) {
            logger.debugf("Intégration Slack non configurée : pas de rattachement pour %s", email);
            return Optional.empty();
        }

        try {
            UsersLookupByEmailResponse response = slackApp.client()
                    .usersLookupByEmail(r -> r.token(token.get()).email(email));
            if (response == null || !response.isOk() || response.getUser() == null) {
                logger.debugf(
                        "Aucun compte Slack pour %s (%s)",
                        email, response == null ? "pas de réponse" : response.getError());
                return Optional.empty();
            }
            return Optional.ofNullable(response.getUser().getId());
        } catch (Exception e) {
            logger.warnf(e, "Appel Slack users.lookupByEmail en erreur pour %s", email);
            return Optional.empty();
        }
    }
}
