package com.zenika.thezaurus.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.repository.UserRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class CurrentUserServiceTest {
    @Inject
    CurrentUserService currentUser;

    @InjectMock
    UserRepository repository;

    @Test
    @TestSecurity(user = "jane@zenika.com", roles = Role.Names.CONSULTANT)
    public void returnsTheAuthenticatedUser() throws Exception {
        User jane = User.builder().name("Jane").email("jane@zenika.com").build();
        Mockito.when(repository.findByEmail("jane@zenika.com")).thenReturn(jane);
        assertEquals(Optional.of(jane), currentUser.getUser());
    }

    @Test
    @TestSecurity(user = "missing@zenika.com", roles = Role.Names.CONSULTANT)
    public void returnsEmptyWhenNoAccountExists() throws Exception {
        assertTrue(currentUser.getUser().isEmpty());
    }

    @Test
    public void returnsEmptyForAnAnonymousIdentity() throws Exception {
        assertTrue(currentUser.getUser().isEmpty());
        Mockito.verifyNoInteractions(repository);
    }
}
