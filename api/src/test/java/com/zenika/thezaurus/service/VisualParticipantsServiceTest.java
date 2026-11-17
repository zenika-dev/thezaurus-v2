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
    void discardsFirstPhotoWhenConsentIsRevokedDuringSecondPhotoRead() throws Exception {
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

        assertEquals("Alice", payload.get(0).get("name"));
        assertFalse(payload.get(0).containsKey("photo"));
        assertTrue(payload.get(1).containsKey("photo"));
    }

    @Test
    void missingPhotoUsesLogoButMissingProfileIsStillNotFound() throws Exception {
        when(storage.read("alice")).thenThrow(new NotFoundException());
        assertFalse(participants.prepare(List.of(alice)).getFirst().containsKey("photo"));
        when(users.findByEmail(alice.email())).thenReturn(null);
        assertThrows(NotFoundException.class, () -> participants.prepare(List.of(alice)));
    }

    @Test
    void refusedPhotoIsNeverRead() throws Exception {
        when(users.findByEmail(alice.email()))
                .thenReturn(alice.toBuilder().aiPhotoAllowed(false).build());
        assertFalse(participants.prepare(List.of(alice)).getFirst().containsKey("photo"));
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

    private static byte[] photo() throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB), "png", output);
        return output.toByteArray();
    }
}
