package com.zenika.thezaurus.util;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.time.Clock;
import java.time.ZoneId;

@Singleton
public class ClockProducer {
    @Produces
    @Singleton
    public Clock clock() {
        return Clock.system(ZoneId.of("Europe/Paris"));
    }
}
