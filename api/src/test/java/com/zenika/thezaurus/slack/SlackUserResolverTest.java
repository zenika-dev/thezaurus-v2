package com.zenika.thezaurus.slack;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.slack.api.RequestConfigurator;
import com.slack.api.bolt.App;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.users.UsersLookupByEmailRequest;
import com.slack.api.methods.request.users.UsersLookupByEmailRequest.UsersLookupByEmailRequestBuilder;
import com.slack.api.methods.response.users.UsersLookupByEmailResponse;
import com.slack.api.model.User;
import com.zenika.thezaurus.repository.UserRepository;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class SlackUserResolverTest {

    private SlackUserResolver resolver;
    private MethodsClient client;
    private UserRepository userRepository;

    @BeforeEach
    public void setUp() {
        App app = Mockito.mock(App.class);
        client = Mockito.mock(MethodsClient.class);
        Mockito.when(app.client()).thenReturn(client);
        userRepository = Mockito.mock(UserRepository.class);

        resolver = new SlackUserResolver();
        resolver.slackApp = app;
        resolver.userRepository = userRepository;
        resolver.botToken = Optional.of("xoxb-token");
    }

    private void respondWith(UsersLookupByEmailResponse response) throws Exception {
        Mockito.when(client.usersLookupByEmail(Mockito.any(RequestConfigurator.class)))
                .thenReturn(response);
    }

    private UsersLookupByEmailResponse okResponse(String slackUserId) {
        User slackUser = new User();
        slackUser.setId(slackUserId);
        UsersLookupByEmailResponse response = new UsersLookupByEmailResponse();
        response.setOk(true);
        response.setUser(slackUser);
        return response;
    }

    @Test
    public void testPersistsResolvedSlackUserId() throws Exception {
        respondWith(okResponse("U123"));

        resolver.resolveAndPersist("jane@zenika.com");

        Mockito.verify(userRepository).updateSlackUserId("jane@zenika.com", "U123");
    }

    @Test
    public void testWritesNothingWhenSlackDoesNotKnowTheEmail() throws Exception {
        UsersLookupByEmailResponse response = new UsersLookupByEmailResponse();
        response.setOk(false);
        response.setError("users_not_found");
        respondWith(response);

        resolver.resolveAndPersist("jane@zenika.com");

        Mockito.verifyNoInteractions(userRepository);
    }

    @Test
    public void testDoesNotCallSlackWhenIntegrationIsNotConfigured() {
        // Sans bot-token, l'App Bolt n'a pas de token : l'appel serait voué à l'échec.
        resolver.botToken = Optional.empty();

        resolver.resolveAndPersist("jane@zenika.com");

        Mockito.verifyNoInteractions(client);
        Mockito.verifyNoInteractions(userRepository);
    }

    @Test
    public void testBlankBotTokenIsTreatedAsNotConfigured() {
        // La configuration a une valeur par défaut vide : « absent » se présente comme « blanc ».
        resolver.botToken = Optional.of("   ");

        resolver.resolveAndPersist("jane@zenika.com");

        Mockito.verifyNoInteractions(client);
    }

    @Test
    public void testSwallowsSlackFailures() throws Exception {
        Mockito.when(client.usersLookupByEmail(Mockito.any(RequestConfigurator.class)))
                .thenThrow(new IOException("Slack injoignable"));

        // Best-effort : l'échec ne remonte pas.
        assertDoesNotThrow(() -> resolver.resolveAndPersist("jane@zenika.com"));
        Mockito.verifyNoInteractions(userRepository);
    }

    @Test
    public void testSwallowsPersistenceFailures() throws Exception {
        respondWith(okResponse("U123"));
        Mockito.doThrow(new RuntimeException("Firestore injoignable"))
                .when(userRepository)
                .updateSlackUserId(Mockito.anyString(), Mockito.anyString());

        assertDoesNotThrow(() -> resolver.resolveAndPersist("jane@zenika.com"));
    }

    @Test
    public void testLooksUpTheAddressItWasGivenWithTheBotToken() throws Exception {
        respondWith(okResponse("U123"));

        resolver.resolveAndPersist("jane@zenika.com");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<RequestConfigurator<UsersLookupByEmailRequestBuilder>> captor =
                ArgumentCaptor.forClass(RequestConfigurator.class);
        Mockito.verify(client).usersLookupByEmail(captor.capture());
        UsersLookupByEmailRequest request =
                captor.getValue().configure(UsersLookupByEmailRequest.builder()).build();
        assertEquals("jane@zenika.com", request.getEmail());
        assertEquals("xoxb-token", request.getToken());
    }
}
