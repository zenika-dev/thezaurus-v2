package com.zenika.thezaurus.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServiceUnavailableException;
import java.util.Optional;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ProfilePhotoStorage {
    @Inject
    Instance<Storage> storage;

    @ConfigProperty(name = "thezaurus.photos.bucket")
    Optional<String> bucket;

    private String bucketName() {
        return bucket.filter(value -> !value.isBlank())
                .orElseThrow(() -> new ServiceUnavailableException("Le stockage des photos n’est pas configuré."));
    }

    public String save(byte[] bytes, String contentType) {
        String object = "profile-photos/" + UUID.randomUUID();
        String targetBucket = bucketName();
        storage.get()
                .create(
                        BlobInfo.newBuilder(targetBucket, object)
                                .setContentType(contentType)
                                .setCacheControl("private, no-store")
                                .build(),
                        bytes);
        return object;
    }

    public byte[] read(String object) {
        String targetBucket = bucketName();
        try {
            return storage.get().readAllBytes(BlobId.of(targetBucket, object));
        } catch (StorageException e) {
            if (e.getCode() == 404) throw new NotFoundException();
            throw e;
        }
    }

    public void delete(String object) {
        if (object == null || object.isBlank()) return;
        String targetBucket = bucketName();
        storage.get().delete(BlobId.of(targetBucket, object));
    }
}
