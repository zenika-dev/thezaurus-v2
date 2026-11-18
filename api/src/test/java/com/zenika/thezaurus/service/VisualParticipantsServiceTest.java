package com.zenika.thezaurus.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.repository.UserRepository;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServiceUnavailableException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VisualParticipantsServiceTest {
    final VisualParticipantsService participants = new VisualParticipantsService();
    final ProfilePhotoService photos = new ProfilePhotoService();
    final UserRepository users = mock(UserRepository.class);
    final ProfilePhotoStorage storage = mock(ProfilePhotoStorage.class);
    final User alice = User.builder()
            .name("Alice")
            .email("alice@example.test")
            .aiPhotoAllowed(true)
            .customPhotoObject("alice")
            .build();
    final User bob = User.builder()
            .name("Bob")
            .email("bob@example.test")
            .aiPhotoAllowed(true)
            .customPhotoObject("bob")
            .build();

    @BeforeEach
    void setup() throws Exception {
        participants.photos = photos;
        photos.users = users;
        photos.storage = storage;
        when(users.findByEmail(alice.email())).thenReturn(alice);
        when(users.findByEmail(bob.email())).thenReturn(bob);
    }

    @Test
    void discardsFirstPhotoWhenAiPhotoAuthorizationIsRevokedDuringSecondPhotoRead() throws Exception {
        var revoked = new AtomicBoolean();
        when(users.findByEmail(alice.email()))
                .thenAnswer(invocation ->
                        alice.toBuilder().aiPhotoAllowed(!revoked.get()).build());
        var bytes = photo();
        when(storage.read("alice")).thenReturn(bytes);
        when(storage.read("bob")).thenAnswer(invocation -> {
            revoked.set(true);
            return bytes;
        });

        var payload = participants.prepare(List.of(alice, bob));

        assertEquals("Alice", payload.get(0).name());
        assertNull(payload.get(0).photo());
        assertNotNull(payload.get(1).photo());
    }

    @Test
    void missingPhotoUsesLogoButMissingProfileIsStillNotFound() throws Exception {
        when(storage.read("alice")).thenThrow(new NotFoundException());
        assertNull(participants.prepare(List.of(alice)).getFirst().photo());
        when(users.findByEmail(alice.email())).thenReturn(null);
        assertThrows(NotFoundException.class, () -> participants.prepare(List.of(alice)));
    }

    @Test
    void refusedPhotoIsNeverRead() throws Exception {
        when(users.findByEmail(alice.email()))
                .thenReturn(alice.toBuilder().aiPhotoAllowed(false).build());
        assertNull(participants.prepare(List.of(alice)).getFirst().photo());
        verifyNoInteractions(storage);
    }

    @Test
    void unavailableStorageIsNotTreatedAsMissingPhoto() {
        when(storage.read("alice")).thenThrow(new ServiceUnavailableException());
        assertThrows(ServiceUnavailableException.class, () -> participants.prepare(List.of(alice)));
    }

    @Test
    void profileDeletedDuringPhotoReadIsStillNotFound() throws Exception {
        when(users.findByEmail(alice.email())).thenReturn(alice, alice, null);
        when(storage.read("alice")).thenReturn(photo());
        assertThrows(NotFoundException.class, () -> participants.prepare(List.of(alice)));
    }

    @Test
    void freeAuthorsAndUnknownProfilesUseLogoWithoutReadingPixels() throws Exception {
        var free = User.builder().name("Auteur libre").build();
        when(users.findByEmail("unknown@example.test")).thenReturn(null);
        var unknown =
                User.builder().name("Externe").email("unknown@example.test").build();
        var result = participants.prepareAuthors(List.of(free, unknown));
        assertEquals("Auteur libre", result.getFirst().name());
        assertEquals("Externe", result.get(1).name());
        assertTrue(result.stream().allMatch(person -> person.photo() == null));
        verifyNoInteractions(storage);
    }

    @Test
    void blankEmbeddedNameUsesCurrentProfileName() throws Exception {
        when(users.findByEmail(alice.email()))
                .thenReturn(alice.toBuilder().aiPhotoAllowed(false).build());
        var result = participants.prepare(List.of(alice.toBuilder().name(" ").build()));
        assertEquals("Alice", result.getFirst().name());
    }

    @Test
    void freeAuthorWithoutNameIsRejected() {
        assertThrows(
                jakarta.ws.rs.BadRequestException.class,
                () -> participants.prepareAuthors(
                        List.of(User.builder().name(" ").build())));
    }

    private static byte[] photo() throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB), "png", output);
        return output.toByteArray();
    }
}
