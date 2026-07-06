package com.zenika.thezaurus.slack.command;

import com.slack.api.bolt.App;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class PingCommand implements SlackCommand {

    @Inject
    Logger logger;

    public static final String PING_COMMAND = "/ping";


    public void register(App app){
        app.command(PING_COMMAND, (request, context) -> {
            logger.info("Ping");
            return context.ack("Pong ! 🏓");
        });
    }

}