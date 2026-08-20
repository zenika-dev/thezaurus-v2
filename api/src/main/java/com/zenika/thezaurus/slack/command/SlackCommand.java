package com.zenika.thezaurus.slack.command;

import com.slack.api.bolt.App;

public interface SlackCommand {
    void register(App app);
}
