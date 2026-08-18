package com.zenika.thezaurus.migration;

import com.zenika.thezaurus.repository.TalkRepository;
import com.zenika.thezaurus.repository.UserRepository;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.concurrent.ExecutionException;

/**
 * Migrations de données jouées au démarrage, avant que l'application ne serve du trafic.
 * Chaque migration est idempotente : elle s'exécute sans effet une fois les données à jour.
 * En cas d'échec, l'application ne démarre pas — la révision précédente continue de servir.
 * Classe temporaire : à supprimer une fois la migration constatée en production (log "0 migré"
 * ou compteur retombé à zéro sur un redémarrage).
 */
@ApplicationScoped
public class LegacyDataMigration {

    @Inject
    Logger logger;

    @Inject
    TalkRepository talkRepository;

    @Inject
    UserRepository userRepository;

    /**
     * Désactivé en profil test : les tests n'ont pas de Firestore (tout est mocké) et
     * l'instanciation du client au démarrage ferait échouer le boot de Quarkus.
     */
    @ConfigProperty(name = "thezaurus.migrations.startup", defaultValue = "true")
    boolean enabled;

    void onStart(@Observes StartupEvent event) throws ExecutionException, InterruptedException {
        if (!enabled) {
            return;
        }
        int migratedTalks = talkRepository.migrateLegacySpeakers();
        if (migratedTalks > 0) {
            logger.infov("Migration speakers legacy : {0} talk(s) réécrit(s) au format User", migratedTalks);
        }
        int migratedUsers = userRepository.migrateLegacyRoles();
        if (migratedUsers > 0) {
            logger.infov("Migration rôles legacy : {0} utilisateur(s) réécrit(s) au format roles[]", migratedUsers);
        }
        int migratedEmails = userRepository.migrateLegacyEmailCasing();
        if (migratedEmails > 0) {
            logger.infov("Migration casse des emails : {0} document(s) utilisateur re-keyé(s) en minuscules", migratedEmails);
        }
    }
}
