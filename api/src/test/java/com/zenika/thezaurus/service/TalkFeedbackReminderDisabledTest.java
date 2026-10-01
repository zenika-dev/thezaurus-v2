package com.zenika.thezaurus.service;

import static org.mockito.Mockito.verifyNoInteractions;

import com.zenika.thezaurus.repository.ReminderTemplateRepository;
import com.zenika.thezaurus.repository.TalkRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TalkFeedbackReminderDisabledTest {
    @Inject
    TalkFeedbackReminderService service;

    @InjectMock
    TalkRepository talks;

    @InjectMock
    ReminderTemplateRepository templates;

    @Test
    void defaultConfigurationDoesNotReadDataOrSendReminders() throws Exception {
        service.sendReminders();

        verifyNoInteractions(talks, templates);
    }
}
