package com.zenika.thezaurus.repository;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.zenika.thezaurus.exception.ThezaurusException;
import com.zenika.thezaurus.model.ReminderTemplateView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ReminderTemplateRepository {
    @Inject
    Firestore firestore;

    @ConfigProperty(name = "thezaurus.firestore.collection.prefix")
    Optional<String> collectionPrefix;

    private DocumentReference document() {
        String prefix = collectionPrefix.orElse("").trim();
        return firestore
                .collection((prefix.isEmpty() ? "" : prefix + "_") + "app_configuration")
                .document("reminder_template");
    }

    public ReminderTemplateView get() throws ExecutionException, InterruptedException {
        return view(document().get().get());
    }

    public ReminderTemplateView save(String subject, String bodyHtml, long revision)
            throws ExecutionException, InterruptedException {
        DocumentReference reference = document();
        try {
            return firestore
                    .runTransaction(transaction -> {
                        ReminderTemplateView current =
                                view(transaction.get(reference).get());
                        if (current.revision() != revision) {
                            throw new ThezaurusException(
                                    "Le modèle a été modifié par un autre administrateur. Rechargez la version enregistrée.",
                                    Response.Status.CONFLICT);
                        }
                        ReminderTemplateView next = new ReminderTemplateView(subject, bodyHtml, revision + 1);
                        transaction.set(
                                reference,
                                Map.of("subject", subject, "bodyHtml", bodyHtml, "revision", next.revision()));
                        return next;
                    })
                    .get();
        } catch (ExecutionException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof ThezaurusException conflict) throw conflict;
            }
            throw exception;
        }
    }

    private static ReminderTemplateView view(DocumentSnapshot snapshot) {
        if (!snapshot.exists()) return new ReminderTemplateView("", "", 0);
        return new ReminderTemplateView(
                snapshot.getString("subject"), snapshot.getString("bodyHtml"), snapshot.getLong("revision"));
    }
}
