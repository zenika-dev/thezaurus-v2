package com.zenika.thezaurus.slack.command;

import com.slack.api.RequestConfigurator;
import com.slack.api.bolt.context.builtin.ViewSubmissionContext;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.users.UsersInfoResponse;
import com.slack.api.model.User;
import com.zenika.thezaurus.service.TalkService;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TalkCommandTest {

    private TalkCommand command;
    private MethodsClient client;
    private ViewSubmissionContext ctx;

    @BeforeEach
    public void setUp() {
        command = new TalkCommand();
        command.logger = Mockito.mock(Logger.class);
        command.service = Mockito.mock(TalkService.class);

        client = Mockito.mock(MethodsClient.class);
        ctx = Mockito.mock(ViewSubmissionContext.class);
        Mockito.when(ctx.client()).thenReturn(client);
    }

    @Test
    public void testGetSpeakersWithNameAndEmail() throws IOException, SlackApiException {
        User slackUser = new User();
        slackUser.setRealName("Jane Doe");
        User.Profile profile = new User.Profile();
        profile.setEmail("jane@zenika.com");
        slackUser.setProfile(profile);

        UsersInfoResponse response = new UsersInfoResponse();
        response.setOk(true);
        response.setUser(slackUser);

        Mockito.when(client.usersInfo(Mockito.any(RequestConfigurator.class))).thenReturn(response);

        List<com.zenika.thezaurus.model.User> speakers = command.getSpeakers(ctx, List.of("U123"));

        assertEquals(1, speakers.size());
        assertEquals("Jane Doe", speakers.get(0).getName());
        assertEquals("jane@zenika.com", speakers.get(0).getEmail());
    }

    @Test
    public void testGetSpeakersWithoutEmail() throws IOException, SlackApiException {
        User slackUser = new User();
        slackUser.setRealName("Jane Doe");

        UsersInfoResponse response = new UsersInfoResponse();
        response.setOk(true);
        response.setUser(slackUser);

        Mockito.when(client.usersInfo(Mockito.any(RequestConfigurator.class))).thenReturn(response);

        List<com.zenika.thezaurus.model.User> speakers = command.getSpeakers(ctx, List.of("U123"));

        assertEquals("Jane Doe", speakers.get(0).getName());
        assertNull(speakers.get(0).getEmail());
    }

    @Test
    public void testGetSpeakersHandlesUnknownUser() throws IOException, SlackApiException {
        UsersInfoResponse response = new UsersInfoResponse();
        response.setOk(false);
        response.setError("user_not_found");

        Mockito.when(client.usersInfo(Mockito.any(RequestConfigurator.class))).thenReturn(response);

        List<com.zenika.thezaurus.model.User> speakers = command.getSpeakers(ctx, List.of("U123"));

        assertEquals(1, speakers.size());
        assertTrue(speakers.get(0).getName().startsWith("Utilisateur Inconnu"));
    }

    @Test
    public void testGetSpeakersHandlesSlackApiError() throws IOException, SlackApiException {
        Mockito.when(client.usersInfo(Mockito.any(RequestConfigurator.class))).thenThrow(new IOException("boom"));

        List<com.zenika.thezaurus.model.User> speakers = command.getSpeakers(ctx, List.of("U123"));

        assertEquals(1, speakers.size());
        assertTrue(speakers.get(0).getName().startsWith("Erreur Réseau"));
    }
}
