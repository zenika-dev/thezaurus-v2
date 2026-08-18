package com.zenika.thezaurus.auth;

import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.repository.UserRepository;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
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

    /** Exécute le supplier en ligne, comme le ferait Quarkus sur un worker thread. */
    private final AuthenticationRequestContext context = supplier -> Uni.createFrom().item(supplier);

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

    private SecurityIdentity augment(SecurityIdentity identity) {
        return augmentor.augment(identity, context).await().indefinitely();
    }

    @Test
    public void testNewUserGetsNameFromSsoClaim() throws Exception {
        Mockito.when(userRepository.findByEmail("new@zenika.com")).thenReturn(null);

        SecurityIdentity result = augment(jwtIdentity("new@zenika.com", "New User"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        Mockito.verify(userRepository).create(captor.capture());
        assertEquals("New User", captor.getValue().name());
        assertEquals("new@zenika.com", captor.getValue().email());
        assertEquals("membre", captor.getValue().role());
        assertEquals("new@zenika.com", result.getPrincipal().getName());
        assertTrue(result.hasRole("membre"));
    }

    @Test
    public void testExistingUserIsReadOnly() throws Exception {
        User existing = User.builder().email("old@zenika.com").name("Already Set").role("admin").build();
        Mockito.when(userRepository.findByEmail("old@zenika.com")).thenReturn(existing);

        SecurityIdentity result = augment(jwtIdentity("old@zenika.com", "Different Name"));

        assertEquals("old@zenika.com", result.getPrincipal().getName());
        assertTrue(result.hasRole("admin"));
        // L'authentification ne doit écrire que sur une création : pas de write par requête.
        Mockito.verify(userRepository, Mockito.never()).update(Mockito.anyString(), Mockito.any());
        Mockito.verify(userRepository, Mockito.never()).create(Mockito.any());
    }

    @Test
    public void testExistingUserWithMissingNameIsBackfilledFromSsoClaim() throws Exception {
        User existing = User.builder().email("legacy@zenika.com").name(null).role("admin").build();
        Mockito.when(userRepository.findByEmail("legacy@zenika.com")).thenReturn(existing);

        SecurityIdentity result = augment(jwtIdentity("legacy@zenika.com", "Legacy User"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        Mockito.verify(userRepository).update(Mockito.eq("legacy@zenika.com"), captor.capture());
        assertEquals("Legacy User", captor.getValue().name());
        assertEquals("admin", captor.getValue().role());
        Mockito.verify(userRepository, Mockito.never()).create(Mockito.any());
        assertEquals("legacy@zenika.com", result.getPrincipal().getName());
        assertTrue(result.hasRole("admin"));
    }

    @Test
    public void testExistingUserWithNameAlreadySetDoesNotTriggerWrite() throws Exception {
        User existing = User.builder().email("old@zenika.com").name("Already Set").role("admin").build();
        Mockito.when(userRepository.findByEmail("old@zenika.com")).thenReturn(existing);

        augment(jwtIdentity("old@zenika.com", "Different Name"));

        Mockito.verify(userRepository, Mockito.never()).update(Mockito.anyString(), Mockito.any());
        Mockito.verify(userRepository, Mockito.never()).create(Mockito.any());
    }

    @Test
    public void testNonZenikaEmailIsNotEnriched() {
        SecurityIdentity result = augment(jwtIdentity("intru@evil.com", "Intru"));

        assertTrue(result.getRoles().isEmpty());
        Mockito.verifyNoInteractions(userRepository);
    }

    @Test
    public void testMockAuthGrantsDevIdentityToAnonymous() {
        augmentor.mockAuth = true;

        SecurityIdentity result = augment(QuarkusSecurityIdentity.builder().setAnonymous(true).build());

        assertEquals("dev@zenika.com", result.getPrincipal().getName());
        assertTrue(result.hasRole("admin"));
        assertTrue(result.hasRole("membre"));
        Mockito.verifyNoInteractions(userRepository);
    }

    @Test
    public void testAnonymousIsUntouchedWithoutMockAuth() {
        SecurityIdentity anonymous = QuarkusSecurityIdentity.builder().setAnonymous(true).build();

        SecurityIdentity result = augment(anonymous);

        assertTrue(result.isAnonymous());
        assertTrue(result.getRoles().isEmpty());
        Mockito.verifyNoInteractions(userRepository);
    }
}
