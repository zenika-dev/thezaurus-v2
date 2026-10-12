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
import com.zenika.thezaurus.model.ConferencePeriod;
import com.zenika.thezaurus.model.Office;
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

    @Inject
    com.zenika.thezaurus.repository.UserRepository userRepository;

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
                .element(plainTextInput(
                        ti -> ti.actionId(Field.TITLE.actionId).placeholder(plainText("Le titre de votre talk"))))
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
                        .options(Arrays.stream(Office.values())
                                .map(office -> option(plainText(office.label()), office.value()))
                                .toList())))
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

        List<String> speakerIds = form.selectedUsers(Field.SPEAKERS);
        if (speakerIds == null || speakerIds.isEmpty()) {
            String submitterId = req.getPayload().getUser() != null
                    ? req.getPayload().getUser().getId()
                    : null;
            if (submitterId != null && !submitterId.isBlank()) {
                speakerIds = List.of(submitterId);
            }
        }
        List<User> speakers = getSpeakers(ctx, speakerIds);

        Talk talk = new Talk(title, description, speakers, office, status, visibility);
        if (conferenceDate != null && !conferenceDate.isBlank()) {
            talk = talk.withDate(conferenceDate);
        }
        if ((conferenceName != null && !conferenceName.isBlank()) || conferenceDate != null) {
            talk = talk.withConference(
                    new Conference(null, conferenceName, ConferencePeriod.singleDay(conferenceDate)));
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
     * Résout le speaker sélectionné dans Slack.
     * Le slackUserId est enregistré dans la collection users, tandis que le speaker embarqué
     * dans le talk conserve uniquement son nom et son email, comme pour les talks créés via l'UI.
     */
    private User getSpeaker(ViewSubmissionContext ctx, String userId) {
        logger.debugf("[TalkCommand] Résolution du speaker Slack pour userId: %s", userId);
        String name = null;
        String email = null;
        try {
            UsersInfoResponse userInfo = ctx.client().usersInfo(r -> r.user(userId));
            logger.debugf(
                    "[TalkCommand] Slack usersInfo pour %s : isOk=%s, error=%s, warning=%s, needed=%s",
                    userId, userInfo.isOk(), userInfo.getError(), userInfo.getWarning(), userInfo.getNeeded());

            if (userInfo.isOk() && userInfo.getUser() != null) {
                name = userInfo.getUser().getRealName();
                if (name == null || name.isBlank()) {
                    name = userInfo.getUser().getName();
                }
                var profile = userInfo.getUser().getProfile();
                logger.debugf(
                        "[TalkCommand] Slack user details: realName=%s, name=%s, profile=%s",
                        userInfo.getUser().getRealName(),
                        userInfo.getUser().getName(),
                        profile != null
                                ? ("email=" + profile.getEmail() + ", displayName=" + profile.getDisplayName())
                                : "null");
                if (profile != null) {
                    email = profile.getEmail();
                }
            } else {
                logger.errorf(
                        "[TalkCommand] Impossible de récupérer l'utilisateur Slack %s : %s",
                        userId, userInfo != null ? userInfo.getError() : "réponse nulle");
            }
        } catch (IOException | SlackApiException e) {
            logger.errorf(e, "[TalkCommand] Erreur d'appel API Slack pour l'utilisateur %s", userId);
        }

        // 1. Si l'email n'a pas pu être récupéré depuis Slack, on tente de le trouver via le slackUserId en base
        if (email == null || email.isBlank()) {
            try {
                User existingUser = userRepository.findBySlackUserId(userId);
                if (existingUser != null) {
                    email = existingUser.email();
                    logger.infof(
                            "[TalkCommand] Utilisateur trouvé en base via slackUserId %s : email=%s, name=%s",
                            userId, email, existingUser.name());
                    if (name == null || name.isBlank()) {
                        name = existingUser.name();
                    }
                } else {
                    logger.infof("[TalkCommand] Aucun utilisateur trouvé en base avec slackUserId=%s", userId);
                }
            } catch (ExecutionException | InterruptedException e) {
                logger.errorf(e, "[TalkCommand] Erreur lors de la recherche par slackUserId %s", userId);
            }
        }

        if (name == null || name.isBlank()) {
            name = "Utilisateur Inconnu (" + userId + ")";
        }

        // 2. Si on a l'email, on synchronise le slackUserId dans la collection users par recherche sur l'email
        if (email != null && !email.isBlank()) {
            try {
                User userInDb = userRepository.findByEmail(email);
                if (userInDb != null) {
                    if (userInDb.slackUserId() == null
                            || !userInDb.slackUserId().equals(userId)) {
                        logger.infof(
                                "[TalkCommand] Mise à jour du slackUserId=%s pour l'utilisateur trouvé par email %s",
                                userId, email);
                        userRepository.updateSlackUserId(email, userId);
                    }
                } else {
                    logger.infof(
                            "[TalkCommand] Création de l'utilisateur %s (%s) avec slackUserId=%s", name, email, userId);
                    userRepository.create(User.builder()
                            .name(name)
                            .email(email)
                            .slackUserId(userId)
                            .build());
                }
            } catch (ExecutionException | InterruptedException e) {
                logger.errorf(e, "[TalkCommand] Erreur lors de la mise à jour du slackUserId pour %s", email);
            }
        } else {
            logger.warnf(
                    "[TalkCommand] Impossible de déterminer l'email pour le speaker Slack %s (%s). Le speaker sera enregistré sans email.",
                    userId, name);
        }

        logger.infof("[TalkCommand] Speaker final pour le talk : name='%s', email='%s'", name, email);
        return User.builder().name(name).email(email).build();
    }
}
