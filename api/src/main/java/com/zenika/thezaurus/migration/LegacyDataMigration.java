package com.zenika.thezaurus.migration;

import com.zenika.thezaurus.repository.ConferenceRepository;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.util.concurrent.ExecutionException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

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
    ConferenceRepository conferenceRepository;

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
        int migratedConferenceDates = conferenceRepository.migrateLegacyDates();
        if (migratedConferenceDates > 0) {
            logger.infov(
                    "Migration dates de conférences : {0} conférence(s) réécrite(s) au format ConferencePeriod",
                    migratedConferenceDates);
        }
    }
}
