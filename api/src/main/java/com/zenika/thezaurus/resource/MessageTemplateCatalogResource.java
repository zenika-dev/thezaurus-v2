package com.zenika.thezaurus.resource;

import com.zenika.thezaurus.model.MessageTemplateDefinition;
import com.zenika.thezaurus.model.MessageTemplateDefinition.TemplateLink;
import com.zenika.thezaurus.model.MessageTemplateDefinition.TemplateToken;
import com.zenika.thezaurus.model.Role;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/admin/message-templates")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed(Role.Names.ADMIN)
public class MessageTemplateCatalogResource {
    @GET
    public List<MessageTemplateDefinition> list() {
        return List.of(new MessageTemplateDefinition(
                "talk-reminder",
                "Email de rappel",
                "Un modèle commun à toute l’application. Le message s’adresse à tous les speakers du talk. Aucun email n’est envoyé depuis cette page.",
                "/api/admin/reminder-template",
                List.of(
                        new TemplateToken("talkTitle", "Titre du talk"),
                        new TemplateToken("talkDate", "Date du talk"),
                        new TemplateToken("conferenceName", "Conférence"),
                        new TemplateToken("talksUrl", "Lien vers Thezaurus")),
                List.of(
                        new TemplateToken("hasConference", "Conférence renseignée"),
                        new TemplateToken("hasDate", "Date renseignée"),
                        new TemplateToken("missingVideo", "Vidéo manquante"),
                        new TemplateToken("missingAudience", "Audience manquante")),
                List.of(new TemplateLink("talksUrl", "Lien vers Thezaurus", "Ouvrir Thezaurus")),
                "{#if hasConference}\nLors de {conferenceName}\n{#else}\nMerci pour votre talk {talkTitle}\n{/if}",
                "Date et conférence absentes donnent un texte vide. Une audience de zéro est renseignée. Le lien ouvre la liste des talks.",
                new MessageTemplateDefinition.PreviewContext(
                        "Talk utilisé pour l’aperçu", "/api/admin/reminder-template/contexts")));
    }
}
