package com.zenika.thezaurus.slack;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.zenika.thezaurus.slack.command.SlackCommand;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Optional;
import java.util.function.Predicate;

@ApplicationScoped
public class SlackAppProducer {

    @ConfigProperty(name = "thezaurus.slack.bot-token")
    Optional<String> botToken;

    @ConfigProperty(name = "thezaurus.slack.signing-secret")
    Optional<String> signingSecret;

    @Inject
    Instance<SlackCommand> slackCommands;

    @Inject
    Logger logger;

    @Produces
    @ApplicationScoped
    public App initSlackApp() {
        Optional<String> token = nonBlank(botToken);
        Optional<String> secret = nonBlank(signingSecret);

        if (token.isEmpty() || secret.isEmpty()) {
            logger.warn("Configuration Slack incomplète (thezaurus.slack.bot-token / thezaurus.slack.signing-secret) : "
                    + "l'intégration Slack est désactivée.");
            return new App(AppConfig.builder()
                    .singleTeamBotToken(null)
                    .signingSecret(null)
                    .build());
        }

        AppConfig config = AppConfig.builder()
                .singleTeamBotToken(token.get())
                .signingSecret(secret.get())
                .build();

        App app = new App(config);

        slackCommands.forEach(command -> command.register(app));

        return app;
    }

    private static Optional<String> nonBlank(Optional<String> value) {
        return value.filter(Predicate.not(String::isBlank));
    }
}