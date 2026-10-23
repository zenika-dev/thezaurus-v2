package com.zenika.thezaurus.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServiceUnavailableException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProfilePhotoStorageTest {
    ProfilePhotoStorage photos;
    Storage client;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        photos = new ProfilePhotoStorage();
        photos.storage = mock(Instance.class);
        client = mock(Storage.class);
        when(photos.storage.get()).thenReturn(client);
        photos.bucket = Optional.of("photos-test");
    }

    @Test
    void usesTheConfiguredBucketAndKeepsObjectNamesPrivateAndUnique() {
        byte[] bytes = {0, 1, -1, 42};
        String object = photos.save(bytes, "image/png");
        var info = ArgumentCaptor.forClass(BlobInfo.class);
        verify(client).create(info.capture(), eq(bytes));
        assertEquals("photos-test", info.getValue().getBucket());
        assertEquals(object, info.getValue().getName());
        assertTrue(object.startsWith("profile-photos/"));
        assertEquals("private, no-store", info.getValue().getCacheControl());
        assertEquals("image/png", info.getValue().getContentType());
        assertNotEquals(object, photos.save(bytes, "image/png"));
        when(client.readAllBytes(BlobId.of("photos-test", object))).thenReturn(bytes);
        assertArrayEquals(bytes, photos.read(object));
        photos.delete(object);
        verify(client).delete(BlobId.of("photos-test", object));
    }

    @Test
    void reportsMissingObjectsAndPropagatesStorageFailures() {
        when(client.readAllBytes(BlobId.of("photos-test", "missing"))).thenThrow(new StorageException(404, "missing"));
        assertThrows(NotFoundException.class, () -> photos.read("missing"));
        when(client.readAllBytes(BlobId.of("photos-test", "unavailable")))
                .thenThrow(new StorageException(503, "unavailable"));
        assertThrows(StorageException.class, () -> photos.read("unavailable"));
    }

    @Test
    void missingBucketFailsBeforeCreatingACloudClient() {
        photos.bucket = Optional.empty();
        assertThrows(ServiceUnavailableException.class, () -> photos.save(new byte[] {1}, "image/png"));
        assertThrows(ServiceUnavailableException.class, () -> photos.read("object"));
        assertThrows(ServiceUnavailableException.class, () -> photos.delete("object"));
        photos.delete(null);
        photos.delete("");
        verifyNoInteractions(client);
        verify(photos.storage, never()).get();
    }
}
