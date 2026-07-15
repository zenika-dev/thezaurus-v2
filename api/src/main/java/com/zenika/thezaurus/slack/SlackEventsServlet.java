package com.zenika.thezaurus.slack;

import com.slack.api.bolt.App;
import com.slack.api.bolt.jakarta_servlet.SlackAppServlet;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/slack/events")
public class SlackEventsServlet extends SlackAppServlet {

    @Inject
    public SlackEventsServlet(App app) {
        super(app);
    }
}