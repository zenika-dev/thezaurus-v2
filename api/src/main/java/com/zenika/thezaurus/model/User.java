package com.zenika.thezaurus.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;

/**
 * <ul>
 *   <li>{@code slackUserId} : identifiant Slack, renseigné uniquement pour les speakers créés via
 *       la commande /talk. Sert de clé de rattrapage lorsque l'email n'a pas pu être récupéré au
 *       moment de la création.</li>
 *   <li>{@code role} : rôle d'autorisation ("admin" ou "membre"), porté uniquement par les User
 *       persistés dans la collection {@code users}. Jamais renseigné sur un speaker embarqué dans
 *       un Talk, et volontairement exclu de la sérialisation JSON : il ne doit pas transiter par
 *       l'API, ni en entrée (injection) ni en sortie (fuite via GET /talks). Firestore utilise son
 *       propre mapper et continue de le persister normalement.</li>
 * </ul>
 */
@Builder
public record User(
        String name,
        String email,
        String slackUserId,
        @JsonIgnore String role) {

    public User withName(String name) {
        return new User(name, email, slackUserId, role);
    }
}
