package com.zenika.thezaurus.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.zenika.thezaurus.model.Conference;
import com.zenika.thezaurus.repository.ConferenceRepository;
import com.zenika.thezaurus.repository.TalkRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConferenceServiceTest {
    private final ConferenceService service = new ConferenceService();

    ConferenceServiceTest() {
        service.repository = mock(ConferenceRepository.class);
        service.talkRepository = mock(TalkRepository.class);
    }

    @Test
    void replacesLegacyCountersAndDefaultsToZeroWithOneGroupedRead() throws Exception {
        Conference first = new Conference("a", "Same name", null);
        Conference second = new Conference("b", "Same name", null);
        second.setSubmittedTalksAmount(42);
        when(service.repository.findAll()).thenReturn(List.of(first, second));
        when(service.talkRepository.statisticsByConference())
                .thenReturn(Map.of(
                        "a", new TalkRepository.ConferenceStatistics(4, 2),
                        "deleted", new TalkRepository.ConferenceStatistics(8, 6)));

        assertEquals(List.of(first, second), service.findAllWithStatistics());
        assertEquals(4, first.getSubmittedTalksAmount());
        assertEquals(2, first.getAcceptedTalksAmount());
        assertEquals(0, second.getSubmittedTalksAmount());
        assertEquals(0, second.getAcceptedTalksAmount());
        verify(service.talkRepository, times(1)).statisticsByConference();
        verifyNoMoreInteractions(service.talkRepository);
    }

    @Test
    void skipsStatisticsForEmptyListAndOrdinaryList() throws Exception {
        when(service.repository.findAll()).thenReturn(List.of());
        assertEquals(List.of(), service.findAllWithStatistics());
        service.findAll();
        verifyNoInteractions(service.talkRepository);
    }
}
