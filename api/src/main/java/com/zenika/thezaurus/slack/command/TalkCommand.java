package com.zenika.thezaurus.slack.command;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;
import static com.slack.api.model.block.element.BlockElements.*;
import static com.slack.api.model.view.Views.*;

import com.slack.api.bolt.App;
import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.bolt.context.builtin.ViewSubmissionContext;
import com.slack.api.bolt.request.builtin.SlashCommandRequest;
import com.slack.api.bolt.request.builtin.ViewSubmissionRequest;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.users.UsersInfoResponse;
import com.slack.api.model.block.InputBlock;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.view.View;
import com.slack.api.model.view.ViewState;
import com.zenika.thezaurus.model.Conference;
import com.zenika.thezaurus.model.Talk;
import com.zenika.thezaurus.model.TalkStatus;
import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.model.Visibility;
import com.zenika.thezaurus.service.TalkService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;
import org.jspecify.annotations.NonNull;

@ApplicationScoped
public class TalkCommand implements SlackCommand {

    public static final String TALK_COMMAND = "/talk";

    private enum Field {
        TITLE("title_block", "title_input"),
        SPEAKERS("speakers_block", "speakers_input"),
        OFFICE("office_block", "office_input"),
        DESCRIPTION("description_block", "description_input"),
        STATUS("status_block", "status_input"),
        VISIBILITY("visibility_block", "visibility_input"),
        CONFERENCE("conference_block", "conference_input"),
        DATE("date_block", "date_input");

        final String blockId;
        final String actionId;

        Field(String blockId, String actionId) {
            this.blockId = blockId;
            this.actionId = actionId;
        }
    }

    private record FormValues(Map<String, Map<String, ViewState.Value>> values) {
        private ViewState.Value valueOf(Field field) {
            return values.get(field.blockId).get(field.actionId);
        }

        String text(Field field) {
            return valueOf(field).getValue();
        }

        String selectedOption(Field field) {
            return valueOf(field).getSelectedOption().getValue();
        }

        List<String> selectedUsers(Field field) {
            return valueOf(field).getSelectedUsers();
        }

        String selectedDate(Field field) {
            return valueOf(field).getSelectedDate();
        }
    }

    @Inject
    Logger logger;

    @Inject
    TalkService service;

    public void register(App app) {
        app.command(TALK_COMMAND, this::command);
        app.viewSubmission("talk-submission-modal", this::talkSubmission);
    }

    public Response command(SlashCommandRequest request, SlashCommandContext context)
            throws SlackApiException, IOException {
        String triggerId = request.getPayload().getTriggerId();

        View modalView = view(v -> v.type("modal")
                .callbackId("talk-submission-modal")
                .title(viewTitle(t -> t.type("plain_text").text("Créer un Talk")))
                .submit(viewSubmit(s -> s.type("plain_text").text("Soumettre")))
                .close(viewClose(c -> c.type("plain_text").text("Annuler")))
                .blocks(asBlocks(
                        titleBlock(),
                        speakersBlock(),
                        officeBlock(),
                        descriptionBlock(),
                        statusBlock(),
                        visibilityBlock(),
                        conferenceBlock(),
                        dateBlock())));

        context.client().viewsOpen(r -> r.triggerId(triggerId).view(modalView));

        return context.ack();
    }

    private InputBlock titleBlock() {
        return input(i -> i.blockId(Field.TITLE.blockId)
                .element(plainTextInput(ti ->
                        ti.actionId(Field.TITLE.actionId).placeholder(plainText("Le titre de votre présentation"))))
                .label(plainText("Titre")));
    }

    private InputBlock speakersBlock() {
        return input(i -> i.blockId(Field.SPEAKERS.blockId)
                .element(multiUsersSelect(
                        mu -> mu.actionId(Field.SPEAKERS.actionId).placeholder(plainText("Sélectionnez les speakers"))))
                .label(plainText("Speakers")));
    }

    private InputBlock officeBlock() {
        return input(i -> i.blockId(Field.OFFICE.blockId)
                .element(staticSelect(s -> s.actionId(Field.OFFICE.actionId)
                        .placeholder(plainText("Choisir une agence"))
                        .options(asOptions(
                                option(plainText("Paris"), "Paris"),
                                option(plainText("Nantes"), "Nantes"),
                                option(plainText("Rennes"), "Rennes"),
                                option(plainText("Bordeaux"), "Bordeaux"),
                                option(plainText("Lyon"), "Lyon"),
                                option(plainText("Lille"), "Lille"),
                                option(plainText("Grenoble"), "Grenoble"),
                                option(plainText("Singapour"), "Singapour"),
                                option(plainText("Montréal"), "Montréal")))))
                .label(plainText("Agence")));
    }

    private InputBlock descriptionBlock() {
        return input(i -> i.blockId(Field.DESCRIPTION.blockId)
                .element(plainTextInput(ti -> ti.actionId(Field.DESCRIPTION.actionId)
                        .multiline(true)
                        .placeholder(plainText("De quoi allez-vous parler ?"))))
                .label(plainText("Abstract / Description")));
    }

    private InputBlock statusBlock() {
        return input(i -> i.blockId(Field.STATUS.blockId)
                .element(staticSelect(s -> s.actionId(Field.STATUS.actionId)
                        .placeholder(plainText("Choisir un statut"))
                        .options(asOptions(statusOptions()))))
                .label(plainText("Statut")));
    }

    private OptionObject[] statusOptions() {
        return Arrays.stream(TalkStatus.values())
                .map(status -> option(plainText(capitalize(status.name())), status.name()))
                .toArray(OptionObject[]::new);
    }

    private static String capitalize(String value) {
        return value.charAt(0) + value.substring(1).toLowerCase();
    }

    private InputBlock visibilityBlock() {
        return input(i -> i.blockId(Field.VISIBILITY.blockId)
                .element(radioButtons(r -> r.actionId(Field.VISIBILITY.actionId)
                        .options(asOptions(
                                option(plainText("Public 🌍"), "PUBLIC"), option(plainText("Privé 🔒"), "PRIVATE")))))
                .label(plainText("Visibilité")));
    }

    private InputBlock conferenceBlock() {
        return input(i -> i.blockId(Field.CONFERENCE.blockId)
                .optional(true)
                .element(plainTextInput(ti -> ti.actionId(Field.CONFERENCE.actionId)
                        .placeholder(plainText("Ex: Devoxx France, Sunny Tech..."))))
                .label(plainText("Conférence cible")));
    }

    private InputBlock dateBlock() {
        return input(i -> i.blockId(Field.DATE.blockId)
                .optional(true)
                .element(datePicker(
                        dp -> dp.actionId(Field.DATE.actionId).placeholder(plainText("Sélectionner une date"))))
                .label(plainText("Date")));
    }

    Response talkSubmission(ViewSubmissionRequest req, ViewSubmissionContext ctx) {
        logger.info(req.getPayload());
        FormValues form = new FormValues(req.getPayload().getView().getState().getValues());

        String title = form.text(Field.TITLE);
        String description = form.text(Field.DESCRIPTION);
        String office = form.selectedOption(Field.OFFICE);
        TalkStatus status = TalkStatus.valueOf(form.selectedOption(Field.STATUS));
        Visibility visibility = Visibility.valueOf(form.selectedOption(Field.VISIBILITY));
        String conferenceName = form.text(Field.CONFERENCE);
        String conferenceDate = form.selectedDate(Field.DATE);

        List<User> speakers = getSpeakers(ctx, form.selectedUsers(Field.SPEAKERS));

        Talk talk = new Talk(title, description, speakers, office, status, visibility);
        if ((conferenceName != null && !conferenceName.isBlank()) || conferenceDate != null) {
            talk = talk.withConference(new Conference(null, conferenceName, conferenceDate));
        }

        try {
            service.create(talk);
        } catch (ExecutionException | InterruptedException e) {
            return ctx.ackWithErrors(Map.of("Error", e.toString()));
        }

        return ctx.ack();
    }

    private @NonNull List<User> getSpeakers(ViewSubmissionContext ctx, List<String> speakerIds) {

        return speakerIds.stream().map(userId -> getSpeaker(ctx, userId)).collect(Collectors.toList());
    }

    /**
     * Le slackUserId est renseigné dans tous les cas, y compris en erreur : c'est la seule clé qui
     * permette de re-résoudre l'email d'un speaker plus tard (cf. notifications par mail).
     */
    private User getSpeaker(ViewSubmissionContext ctx, String userId) {
        try {
            UsersInfoResponse userInfo = ctx.client().usersInfo(r -> r.user(userId));

            if (!userInfo.isOk() || userInfo.getUser() == null) {
                logger.error("Impossible de récupérer l'utilisateur " + userId + " : " + userInfo.getError());
                return User.builder()
                        .name("Utilisateur Inconnu (" + userId + ")")
                        .slackUserId(userId)
                        .build();
            }
            String email = userInfo.getUser().getProfile() != null
                    ? userInfo.getUser().getProfile().getEmail()
                    : null;
            if (email == null || email.isBlank()) {
                logger.warn("Aucun email pour l'utilisateur " + userId
                        + " : vérifier que le scope users:read.email est accordé au bot");
            }
            return User.builder()
                    .name(userInfo.getUser().getRealName())
                    .email(email)
                    .slackUserId(userId)
                    .build();
        } catch (IOException | SlackApiException e) {
            logger.error("Erreur d'appel API Slack pour l'utilisateur " + userId, e);
            return User.builder()
                    .name("Erreur Réseau (" + userId + ")")
                    .slackUserId(userId)
                    .build();
        }
    }
}
