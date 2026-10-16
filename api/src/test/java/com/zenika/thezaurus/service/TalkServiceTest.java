package com.zenika.thezaurus.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zenika.thezaurus.model.PagedResult;
import com.zenika.thezaurus.model.Talk;
import com.zenika.thezaurus.model.TalkStatus;
import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.model.Visibility;
import com.zenika.thezaurus.repository.TalkRepository;
import io.quarkus.security.identity.SecurityIdentity;
import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TalkServiceTest {

    private TalkRepository repository;
    private SecurityIdentity identity;
    private TalkService service;

    @BeforeEach
    void setUp() {
        repository = mock(TalkRepository.class);
        identity = mock(SecurityIdentity.class);
        service = new TalkService();
        service.repository = repository;
        service.identity = identity;
    }

    private Talk createTalk(String id, String title, String date, TalkStatus status, String speakerEmail) {
        return new Talk(
                id,
                title,
                "Description",
                List.of(User.builder().name("Speaker").email(speakerEmail).build()),
                "nantes",
                null,
                status,
                Visibility.PUBLIC,
                "quickie",
                date,
                "FR",
                null,
                null,
                null,
                null);
    }

    @Test
    void findPagedDelegatesToRepositoryWithoutPersonalFilter() throws Exception {
        Talk t1 = createTalk("1", "Talk A", "2026-06-01", TalkStatus.PLANNED, "a@zenika.com");
        PagedResult<Talk> expected = new PagedResult<>(List.of(t1), 1, 0, 25, 1);
        when(repository.findPaged(0, 25, TalkStatus.PLANNED, null)).thenReturn(expected);

        PagedResult<Talk> result = service.findPaged(0, 25, TalkStatus.PLANNED, false);

        assertEquals(expected, result);
        verify(repository).findPaged(0, 25, TalkStatus.PLANNED, null);
    }

    @Test
    void findPagedPassesCurrentUserEmailWhenPersonalIsTrue() throws Exception {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("dev@zenika.com");
        when(identity.getPrincipal()).thenReturn(principal);

        Talk t1 = createTalk("1", "Talk 1", "2026-01-01", TalkStatus.PLANNED, "dev@zenika.com");
        PagedResult<Talk> expected = new PagedResult<>(List.of(t1), 1, 0, 25, 1);
        when(repository.findPaged(0, 25, TalkStatus.PLANNED, "dev@zenika.com")).thenReturn(expected);

        PagedResult<Talk> result = service.findPaged(0, 25, TalkStatus.PLANNED, true);

        assertEquals(expected, result);
        verify(repository).findPaged(0, 25, TalkStatus.PLANNED, "dev@zenika.com");
    }
}
