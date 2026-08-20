package com.zenika.thezaurus.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.repository.UserRepository;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import java.util.List;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class IapSecurityAugmentorTest {

    private IapSecurityAugmentor augmentor;
    private UserRepository userRepository;

    /** Exécute le supplier en ligne, comme le ferait Quarkus sur un worker thread. */
    private final AuthenticationRequestContext context =
            supplier -> Uni.createFrom().item(supplier);

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
    public void testNewUserIsCreatedWithConsultantRoleAndSsoName() throws Exception {
        Mockito.when(userRepository.findByEmail("new@zenika.com")).thenReturn(null);

        SecurityIdentity result = augment(jwtIdentity("new@zenika.com", "New User"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        Mockito.verify(userRepository).create(captor.capture());
        assertEquals("New User", captor.getValue().name());
        assertEquals("new@zenika.com", captor.getValue().email());
        assertEquals(List.of(Role.CONSULTANT), captor.getValue().roles());
        assertEquals("new@zenika.com", result.getPrincipal().getName());
        assertEquals(List.of(Role.Names.CONSULTANT), List.copyOf(result.getRoles()));
    }

    @Test
    public void testUserWithMultipleRolesGetsAllRolesOnIdentity() throws Exception {
        User user = User.builder()
                .email("maxime.mainguet@zenika.com")
                .name("Maxime")
                .roles(List.of(Role.CONSULTANT, Role.DT))
                .build();
        Mockito.when(userRepository.findByEmail("maxime.mainguet@zenika.com")).thenReturn(user);

        SecurityIdentity result = augment(jwtIdentity("maxime.mainguet@zenika.com", "Maxime"));

        assertEquals("maxime.mainguet@zenika.com", result.getPrincipal().getName());
        assertTrue(result.getRoles().contains(Role.Names.CONSULTANT));
        assertTrue(result.getRoles().contains(Role.Names.DT));
        assertEquals(2, result.getRoles().size());
    }

    @Test
    public void testExistingUserIsReadOnly() throws Exception {
        User existing = User.builder()
                .email("old@zenika.com")
                .name("Already Set")
                .roles(List.of(Role.ADMIN))
                .build();
        Mockito.when(userRepository.findByEmail("old@zenika.com")).thenReturn(existing);

        SecurityIdentity result = augment(jwtIdentity("old@zenika.com", "Different Name"));

        assertEquals("old@zenika.com", result.getPrincipal().getName());
        assertTrue(result.hasRole(Role.Names.ADMIN));
        // L'authentification ne doit écrire que sur une création : pas de write par requête.
        Mockito.verify(userRepository, Mockito.never()).update(Mockito.anyString(), Mockito.any());
        Mockito.verify(userRepository, Mockito.never()).create(Mockito.any());
    }

    @Test
    public void testExistingUserWithMissingNameIsBackfilledFromSsoClaim() throws Exception {
        User existing = User.builder()
                .email("legacy@zenika.com")
                .name(null)
                .roles(List.of(Role.ADMIN))
                .build();
        Mockito.when(userRepository.findByEmail("legacy@zenika.com")).thenReturn(existing);

        SecurityIdentity result = augment(jwtIdentity("legacy@zenika.com", "Legacy User"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        Mockito.verify(userRepository).update(Mockito.eq("legacy@zenika.com"), captor.capture());
        assertEquals("Legacy User", captor.getValue().name());
        assertEquals(List.of(Role.ADMIN), captor.getValue().roles());
        Mockito.verify(userRepository, Mockito.never()).create(Mockito.any());
        assertEquals("legacy@zenika.com", result.getPrincipal().getName());
        assertTrue(result.hasRole(Role.Names.ADMIN));
    }

    @Test
    public void testUserWithoutRolesGetsNoRoleOnIdentity() throws Exception {
        // Cas défensif : un document users sans champ roles (ne devrait pas exister après
        // migration) ne doit pas faire échouer l'authentification.
        User noRoles = User.builder().email("empty@zenika.com").name("Empty").build();
        Mockito.when(userRepository.findByEmail("empty@zenika.com")).thenReturn(noRoles);

        SecurityIdentity result = augment(jwtIdentity("empty@zenika.com", "Empty"));

        assertTrue(result.getRoles().isEmpty());
    }

    @Test
    public void testUppercaseEmailIsNormalizedAndEnriched() throws Exception {
        // L'email sert de clé de document Firestore et de principal : la casse du JWT ne doit
        // ni refuser l'accès ni créer un doublon.
        User user = User.builder()
                .email("jane@zenika.com")
                .name("Jane")
                .roles(List.of(Role.CONSULTANT))
                .build();
        Mockito.when(userRepository.findByEmail("jane@zenika.com")).thenReturn(user);

        SecurityIdentity result = augment(jwtIdentity("JANE@ZENIKA.COM", "Jane"));

        Mockito.verify(userRepository).findByEmail("jane@zenika.com");
        assertEquals("jane@zenika.com", result.getPrincipal().getName());
        assertTrue(result.hasRole(Role.Names.CONSULTANT));
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

        SecurityIdentity result =
                augment(QuarkusSecurityIdentity.builder().setAnonymous(true).build());

        assertEquals("dev@zenika.com", result.getPrincipal().getName());
        assertTrue(result.hasRole(Role.Names.ADMIN));
        assertTrue(result.hasRole(Role.Names.CONSULTANT));
        Mockito.verifyNoInteractions(userRepository);
    }

    @Test
    public void testAnonymousIsUntouchedWithoutMockAuth() {
        SecurityIdentity anonymous =
                QuarkusSecurityIdentity.builder().setAnonymous(true).build();

        SecurityIdentity result = augment(anonymous);

        assertTrue(result.isAnonymous());
        assertTrue(result.getRoles().isEmpty());
        Mockito.verifyNoInteractions(userRepository);
    }
}
