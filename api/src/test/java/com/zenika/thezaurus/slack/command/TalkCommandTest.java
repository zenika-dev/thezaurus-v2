package com.zenika.thezaurus.slack.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.slack.api.RequestConfigurator;
import com.slack.api.app_backend.views.payload.ViewSubmissionPayload;
import com.slack.api.bolt.context.builtin.ViewSubmissionContext;
import com.slack.api.bolt.request.builtin.ViewSubmissionRequest;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.users.UsersInfoRequest;
import com.slack.api.methods.response.users.UsersInfoResponse;
import com.slack.api.model.view.View;
import com.slack.api.model.view.ViewState;
import com.zenika.thezaurus.model.Talk;
import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.repository.UserRepository;
import com.zenika.thezaurus.service.TalkService;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class TalkCommandTest {

    private TalkCommand command;
    private MethodsClient client;
    private ViewSubmissionContext ctx;
    private TalkService service;
    private UserRepository userRepository;

    @BeforeEach
    public void setUp() {
        command = new TalkCommand();
        command.logger = Mockito.mock(Logger.class);
        service = Mockito.mock(TalkService.class);
        command.service = service;
        userRepository = Mockito.mock(UserRepository.class);
        command.userRepository = userRepository;

        client = Mockito.mock(MethodsClient.class);
        ctx = Mockito.mock(ViewSubmissionContext.class);
        Mockito.when(ctx.client()).thenReturn(client);
    }

    // --- Construction d'une soumission de modale ---------------------------------------------

    private ViewState.Value text(String value) {
        ViewState.Value v = new ViewState.Value();
        v.setValue(value);
        return v;
    }

    private ViewState.Value option(String value) {
        ViewState.SelectedOption selected = new ViewState.SelectedOption();
        selected.setValue(value);
        ViewState.Value v = new ViewState.Value();
        v.setSelectedOption(selected);
        return v;
    }

    private ViewState.Value users(List<String> userIds) {
        ViewState.Value v = new ViewState.Value();
        v.setSelectedUsers(userIds);
        return v;
    }

    private ViewSubmissionRequest submissionWithSpeakers(List<String> speakerIds) {
        Map<String, Map<String, ViewState.Value>> values = new HashMap<>();
        values.put("title_block", Map.of("title_input", text("Mon talk")));
        values.put("speakers_block", Map.of("speakers_input", users(speakerIds)));
        values.put("office_block", Map.of("office_input", option("nantes")));
        values.put("description_block", Map.of("description_input", text("Une description")));
        values.put("status_block", Map.of("status_input", option("DRAFT")));
        values.put("visibility_block", Map.of("visibility_input", option("PUBLIC")));
        values.put("conference_block", Map.of("conference_input", text(null)));
        values.put("date_block", Map.of("date_input", new ViewState.Value()));

        ViewState state = new ViewState();
        state.setValues(values);
        View view = new View();
        view.setState(state);
        ViewSubmissionPayload payload = new ViewSubmissionPayload();
        payload.setView(view);

        ViewSubmissionRequest request = Mockito.mock(ViewSubmissionRequest.class);
        Mockito.when(request.getPayload()).thenReturn(payload);
        return request;
    }

    /** Soumet la modale et renvoie le Talk transmis au service. */
    private Talk submit(List<String> speakerIds) throws Exception {
        command.talkSubmission(submissionWithSpeakers(speakerIds), ctx);
        ArgumentCaptor<Talk> captor = ArgumentCaptor.forClass(Talk.class);
        Mockito.verify(service).create(captor.capture());
        return captor.getValue();
    }

    private void slackReturns(com.slack.api.model.User slackUser) throws IOException, SlackApiException {
        UsersInfoResponse response = new UsersInfoResponse();
        response.setOk(true);
        response.setUser(slackUser);
        Mockito.when(client.usersInfo(Mockito.any(RequestConfigurator.class))).thenReturn(response);
    }

    private com.slack.api.model.User slackUser(String realName, String email, boolean withProfile) {
        com.slack.api.model.User user = new com.slack.api.model.User();
        user.setRealName(realName);
        if (withProfile) {
            com.slack.api.model.User.Profile profile = new com.slack.api.model.User.Profile();
            profile.setEmail(email);
            user.setProfile(profile);
        }
        return user;
    }

    // --- Tests --------------------------------------------------------------------------------

    @Test
    public void testSubmissionCapturesNameEmailAndSyncsSlackIdInUserRepository() throws Exception {
        slackReturns(slackUser("Jane Doe", "jane@zenika.com", true));
        User userInDb = User.builder().name("Jane Doe").email("jane@zenika.com").build();
        Mockito.when(userRepository.findByEmail("jane@zenika.com")).thenReturn(userInDb);

        Talk talk = submit(List.of("U123"));

        assertEquals("Mon talk", talk.title());
        assertEquals(1, talk.speakers().size());
        User speaker = talk.speakers().get(0);
        assertEquals("Jane Doe", speaker.name());
        assertEquals("jane@zenika.com", speaker.email());
        assertNull(speaker.slackUserId(), "Le slackUserId ne doit pas être embarqué dans le Talk");
        assertNull(speaker.roles(), "Un speaker ne doit jamais porter de rôles d'autorisation");

        Mockito.verify(userRepository).updateSlackUserId("jane@zenika.com", "U123");
    }

    @Test
    public void testSubmissionCreatesUserIfNotFoundInUserRepository() throws Exception {
        slackReturns(slackUser("Jane Doe", "jane@zenika.com", true));
        Mockito.when(userRepository.findByEmail("jane@zenika.com")).thenReturn(null);

        Talk talk = submit(List.of("U123"));

        assertEquals(1, talk.speakers().size());
        User speaker = talk.speakers().get(0);
        assertEquals("Jane Doe", speaker.name());
        assertEquals("jane@zenika.com", speaker.email());
        assertNull(speaker.slackUserId());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        Mockito.verify(userRepository).create(captor.capture());
        assertEquals("Jane Doe", captor.getValue().name());
        assertEquals("jane@zenika.com", captor.getValue().email());
        assertEquals("U123", captor.getValue().slackUserId());
    }

    @Test
    public void testSubmissionQueriesTheRightSlackUser() throws Exception {
        slackReturns(slackUser("Jane Doe", "jane@zenika.com", true));

        submit(List.of("U123"));

        // Le RequestConfigurator n'est jamais invoqué par le mock : on l'applique nous-mêmes
        // pour vérifier que c'est bien l'identifiant du speaker qui est interrogé.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<RequestConfigurator<UsersInfoRequest.UsersInfoRequestBuilder>> captor =
                ArgumentCaptor.forClass(RequestConfigurator.class);
        Mockito.verify(client).usersInfo(captor.capture());
        UsersInfoRequest built =
                captor.getValue().configure(UsersInfoRequest.builder()).build();
        assertEquals("U123", built.getUser());
    }

    @Test
    public void testSubmissionWithoutProfileResolvesFromSlackUserIdInUserRepository() throws Exception {
        slackReturns(slackUser("Jane Doe", null, false));
        User userInDb = User.builder()
                .name("Jane Doe")
                .email("jane@zenika.com")
                .slackUserId("U123")
                .build();
        Mockito.when(userRepository.findBySlackUserId("U123")).thenReturn(userInDb);
        Mockito.when(userRepository.findByEmail("jane@zenika.com")).thenReturn(userInDb);

        User speaker = submit(List.of("U123")).speakers().get(0);

        assertEquals("Jane Doe", speaker.name());
        assertEquals("jane@zenika.com", speaker.email());
        assertNull(speaker.slackUserId());
    }

    @Test
    public void testSubmissionWithProfileButNoEmailFallbackWhenNotInDb() throws Exception {
        // Cas où le scope users:read.email n'est pas accordé et l'utilisateur n'est pas en base
        slackReturns(slackUser("Jane Doe", null, true));
        Mockito.when(userRepository.findBySlackUserId("U123")).thenReturn(null);

        User speaker = submit(List.of("U123")).speakers().get(0);

        assertEquals("Jane Doe", speaker.name());
        assertNull(speaker.email());
        assertNull(speaker.slackUserId());
    }

    @Test
    public void testSubmissionHandlesUnknownUser() throws Exception {
        UsersInfoResponse response = new UsersInfoResponse();
        response.setOk(false);
        response.setError("user_not_found");
        Mockito.when(client.usersInfo(Mockito.any(RequestConfigurator.class))).thenReturn(response);

        User speaker = submit(List.of("U123")).speakers().get(0);

        assertTrue(speaker.name().contains("U123"));
        assertNull(speaker.email());
        assertNull(speaker.slackUserId());
    }

    @Test
    public void testSubmissionHandlesSlackApiError() throws Exception {
        Mockito.when(client.usersInfo(Mockito.any(RequestConfigurator.class))).thenThrow(new IOException("boom"));

        User speaker = submit(List.of("U123")).speakers().get(0);

        assertTrue(speaker.name().contains("U123"));
        assertNull(speaker.slackUserId());
    }

    @Test
    public void testSubmissionWithoutSpeakers() throws Exception {
        Talk talk = submit(List.of());

        assertTrue(talk.speakers().isEmpty());
        Mockito.verifyNoInteractions(client);
    }

    @Test
    public void testSubmissionWithMultipleSpeakersIncludingOneFailure() throws Exception {
        UsersInfoResponse ok = new UsersInfoResponse();
        ok.setOk(true);
        ok.setUser(slackUser("Jane Doe", "jane@zenika.com", true));
        UsersInfoResponse ko = new UsersInfoResponse();
        ko.setOk(false);
        ko.setError("user_not_found");
        Mockito.when(client.usersInfo(Mockito.any(RequestConfigurator.class))).thenReturn(ok, ko);

        List<User> speakers = submit(List.of("U123", "U456")).speakers();

        assertEquals(2, speakers.size());
        assertEquals("jane@zenika.com", speakers.get(0).email());
        assertNull(speakers.get(1).email());
        assertNull(speakers.get(1).slackUserId());
    }
}
