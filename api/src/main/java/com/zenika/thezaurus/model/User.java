package com.zenika.thezaurus.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import lombok.Builder;

/**
 * <ul>
 *   <li>{@code slackUserId} : identifiant Slack, posé par la commande /talk ou résolu par email
 *       au login (cf. {@code SlackUserResolver}). Destinataire du canal de notification Slack.</li>
 *   <li>{@code roles} : rôles d'autorisation multi-valués, portés uniquement par les User
 *       persistés dans la collection {@code users}. Jamais renseignés sur un speaker embarqué
 *       dans un Talk, et volontairement exclus de la sérialisation JSON : ils ne doivent pas
 *       transiter par l'API générale, ni en entrée (injection) ni en sortie (fuite via GET /talks
 *       ou GET /api/users). Seule l'administration ({@code UserAdminResource}) les expose, via un
 *       DTO dédié réservé aux admins. Firestore utilise son propre mapper et continue de les
 *       persister normalement.</li>
 *   <li>{@code emailNotificationsEnabled} / {@code slackNotificationsEnabled} : préférences de
 *       notification, exclues du JSON comme {@code roles}. {@link Boolean} et non {@code boolean} :
 *       le mapper Firestore passe {@code null} pour un champ absent, et {@code null} (jamais
 *       exprimée) reste distinct de {@code false} (refusée). Lire via {@link #notifiesByEmail()}
 *       et {@link #notifiesOnSlack()}.</li>
 * </ul>
 */
@Builder
public record User(
        String name,
        String email,
        String slackUserId,
        @JsonIgnore List<Role> roles,
        @JsonIgnore Boolean emailNotificationsEnabled,
        @JsonIgnore Boolean slackNotificationsEnabled) {

    public boolean notifiesByEmail() {
        return Boolean.TRUE.equals(emailNotificationsEnabled);
    }

    public boolean notifiesOnSlack() {
        return Boolean.TRUE.equals(slackNotificationsEnabled);
    }

    public User withName(String name) {
        return new User(name, email, slackUserId, roles, emailNotificationsEnabled, slackNotificationsEnabled);
    }

    public User withRoles(List<Role> roles) {
        return new User(name, email, slackUserId, roles, emailNotificationsEnabled, slackNotificationsEnabled);
    }
}
