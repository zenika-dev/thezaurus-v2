package com.zenika.thezaurus.resource;

import com.zenika.thezaurus.model.MessageTemplateDefinition;
import com.zenika.thezaurus.model.MessageTemplateDefinition.TemplateLink;
import com.zenika.thezaurus.model.MessageTemplateDefinition.TemplateToken;
import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.service.ReminderTemplateMetadata;
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
        return List.of(
                new MessageTemplateDefinition(
                        "talk-reminder",
                        "Email de rappel",
                        "Un modèle commun à toute l’application. Le message s’adresse à tous les speakers du talk. Aucun email n’est envoyé depuis cette page.",
                        "/api/admin/reminder-template",
                        true,
                        "html",
                        List.of(
                                new TemplateToken("speakers", "Noms des speakers"),
                                new TemplateToken("talkTitle", "Titre du talk"),
                                new TemplateToken("talkDate", "Date du talk"),
                                new TemplateToken("conferenceName", "Conférence"),
                                new TemplateToken("talksUrl", "Lien vers Thezaurus")),
                        ReminderTemplateMetadata.CONDITIONS,
                        List.of(new TemplateLink("talksUrl", "Lien vers Thezaurus", "Ouvrir Thezaurus")),
                        "{#if hasConference}\nLors de {conferenceName}\n{#else}\nMerci pour votre talk {talkTitle}\n{/if}",
                        "Date et conférence absentes donnent un texte vide. Une audience de zéro est renseignée. Le lien ouvre la liste des talks.",
                        new MessageTemplateDefinition.PreviewContext(
                                "Talk utilisé pour l’aperçu", "/api/admin/reminder-template/contexts")),
                new MessageTemplateDefinition(
                        "talk-slack-reminder",
                        "Rappel Slack",
                        "Un modèle commun à toute l’application. Le message est envoyé par message direct Slack à chaque speaker. Aucun message n’est envoyé depuis cette page.",
                        "/api/admin/slack-reminder-template",
                        false,
                        "markdown",
                        List.of(
                                new TemplateToken("recipientMention", "Mention Slack du destinataire"),
                                new TemplateToken("speakerName", "Nom du destinataire"),
                                new TemplateToken("speakers", "Noms des speakers"),
                                new TemplateToken("talkTitle", "Titre du talk"),
                                new TemplateToken("talkDate", "Date du talk"),
                                new TemplateToken("conferenceName", "Conférence"),
                                new TemplateToken("talkUrl", "Lien vers Thezaurus"),
                                new TemplateToken("talksUrl", "Lien vers la liste des talks")),
                        ReminderTemplateMetadata.CONDITIONS,
                        List.of(new TemplateLink("talkUrl", "Lien vers Thezaurus", "Ouvrir Thezaurus")),
                        "Bonjour {recipientMention},\n{#if hasConference}\nFélicitations pour ton talk à {conferenceName} !\n{#else}\nFélicitations pour ton talk *{talkTitle}* !\n{/if}\nPense à renseigner le replay et l'audience sur Thezaurus : <{talkUrl}|Ouvrir Thezaurus>.",
                        "Date et conférence absentes donnent un texte vide. Une audience de zéro est renseignée. Le lien ouvre Thezaurus.",
                        new MessageTemplateDefinition.PreviewContext(
                                "Talk utilisé pour l’aperçu", "/api/admin/slack-reminder-template/contexts")));
    }
}
