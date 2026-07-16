package com.zenika.thezaurus.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;

import java.util.List;

/**
 * <ul>
 *   <li>{@code slackUserId} : identifiant Slack, renseigné uniquement pour les speakers créés via
 *       la commande /talk. Sert de clé de rattrapage lorsque l'email n'a pas pu être récupéré au
 *       moment de la création.</li>
 *   <li>{@code roles} : rôles d'autorisation multi-valués, portés uniquement par les User
 *       persistés dans la collection {@code users}. Jamais renseignés sur un speaker embarqué
 *       dans un Talk, et volontairement exclus de la sérialisation JSON : ils ne doivent pas
 *       transiter par l'API générale, ni en entrée (injection) ni en sortie (fuite via GET /talks
 *       ou GET /api/users). Seule l'administration ({@code UserAdminResource}) les expose, via un
 *       DTO dédié réservé aux admins. Firestore utilise son propre mapper et continue de les
 *       persister normalement.</li>
 * </ul>
 */
@Builder
public record User(
        String name,
        String email,
        String slackUserId,
        @JsonIgnore List<Role> roles) {

    public User withName(String name) {
        return new User(name, email, slackUserId, roles);
    }

    public User withRoles(List<Role> roles) {
        return new User(name, email, slackUserId, roles);
    }
}
