package com.zenika.thezaurus.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class User {

    private String name;
    private String email;

    /**
     * Identifiant Slack, renseigné uniquement pour les speakers créés via la commande /talk.
     * Sert de clé de rattrapage lorsque l'email n'a pas pu être récupéré au moment de la création.
     */
    private String slackUserId;

    /**
     * Rôle d'autorisation ("admin" ou "membre"), porté uniquement par les User persistés dans
     * la collection {@code users}. Jamais renseigné sur un speaker embarqué dans un Talk, et
     * volontairement exclu de la sérialisation JSON : il ne doit pas transiter par l'API, ni en
     * entrée (injection) ni en sortie (fuite via GET /talks). Firestore utilise son propre mapper
     * et continue de le persister normalement.
     */
    @JsonIgnore
    private String role;

    @Builder
    private User(String name, String email, String slackUserId, String role) {
        this.name = name;
        this.email = email;
        this.slackUserId = slackUserId;
        this.role = role;
    }
}
