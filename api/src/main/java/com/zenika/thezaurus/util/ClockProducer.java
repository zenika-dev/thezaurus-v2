package com.zenika.thezaurus.util;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.time.Clock;
import java.time.ZoneId;

// Fournit une horloge injectable (remplaçable dans les tests).
@Singleton
public class ClockProducer {
    @Produces
    @Singleton
    public Clock clock() {
        return Clock.system(ZoneId.of("Europe/Paris"));
    }
}
