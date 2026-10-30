package com.zenika.thezaurus.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.zenika.thezaurus.model.Conference;
import com.zenika.thezaurus.repository.ConferenceRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConferenceServiceTest {
    @Test
    void statisticsOnlyReadTheConferenceCatalogue() throws Exception {
        ConferenceService service = new ConferenceService();
        service.repository = mock(ConferenceRepository.class);
        Conference conference = new Conference("a", "Conference", null);
        conference.setSubmittedTalksAmount(4);
        conference.setAcceptedTalksAmount(2);
        when(service.repository.findAll()).thenReturn(List.of(conference));
        assertEquals(List.of(conference), service.findAllWithStatistics());
        verify(service.repository).findAll();
        verifyNoMoreInteractions(service.repository);
    }
}
