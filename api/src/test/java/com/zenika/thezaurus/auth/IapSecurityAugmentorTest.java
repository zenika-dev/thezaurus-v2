package com.zenika.thezaurus.auth;

import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.repository.UserRepository;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IapSecurityAugmentorTest {

    private IapSecurityAugmentor augmentor;
    private UserRepository userRepository;

    @BeforeEach
    public void setUp() {
        augmentor = new IapSecurityAugmentor();
        userRepository = Mockito.mock(UserRepository.class);
        augmentor.userRepository = userRepository;
        augmentor.mockAuth = false;
    }

    private SecurityIdentity jwtIdentity(String email, String name) {
        JsonWebToken jwt = Mockito.mock(JsonWebToken.class);
        Mockito.when(jwt.getClaim("email")).thenReturn(email);
        Mockito.when(jwt.getClaim("name")).thenReturn(name);
        Mockito.when(jwt.getName()).thenReturn(email);
        return QuarkusSecurityIdentity.builder().setPrincipal(jwt).build();
    }

    @Test
    public void testNewUserGetsNameFromSsoClaim() throws Exception {
        Mockito.when(userRepository.findByEmail("new@zenika.com")).thenReturn(null);

        SecurityIdentity result = augmentor.augment(jwtIdentity("new@zenika.com", "New User"), null)
                .await().indefinitely();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        Mockito.verify(userRepository).create(captor.capture());
        assertEquals("New User", captor.getValue().getName());
        assertEquals("new@zenika.com", captor.getValue().getEmail());
        assertEquals("membre", captor.getValue().getRole());
        assertEquals("new@zenika.com", result.getPrincipal().getName());
    }

    @Test
    public void testExistingUserWithoutNameIsBackfilledFromSso() throws Exception {
        User existing = new User("old@zenika.com", "admin");
        Mockito.when(userRepository.findByEmail("old@zenika.com")).thenReturn(existing);

        augmentor.augment(jwtIdentity("old@zenika.com", "Old User"), null).await().indefinitely();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        Mockito.verify(userRepository).update(Mockito.eq("old@zenika.com"), captor.capture());
        assertEquals("Old User", captor.getValue().getName());
    }

    @Test
    public void testExistingUserWithNameIsNotUpdated() throws Exception {
        User existing = new User("old@zenika.com", "admin");
        existing.setName("Already Set");
        Mockito.when(userRepository.findByEmail("old@zenika.com")).thenReturn(existing);

        augmentor.augment(jwtIdentity("old@zenika.com", "Different Name"), null).await().indefinitely();

        Mockito.verify(userRepository, Mockito.never()).update(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void testNonZenikaEmailIsNotEnriched() {
        SecurityIdentity result = augmentor.augment(jwtIdentity("intru@evil.com", "Intru"), null)
                .await().indefinitely();

        assertTrue(result.getRoles().isEmpty());
        Mockito.verifyNoInteractions(userRepository);
    }
}
