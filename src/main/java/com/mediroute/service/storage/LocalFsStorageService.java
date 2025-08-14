package com.mediroute.service.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Service
public class LocalFsStorageService implements StorageService {

    private final Path baseDir;

    public LocalFsStorageService(@Value("${storage.local.base-dir:./var/storage}") String baseDir) {
        this.baseDir = Path.of(baseDir).toAbsolutePath().normalize();
    }

    @Override
    public StoredObject store(InputStream data, String contentType, String suggestedName, Map<String, String> metadata) {
        try {
            LocalDate now = LocalDate.now();
            String fileName = (suggestedName == null || suggestedName.isBlank()) ? UUID.randomUUID() + "" : suggestedName;
            String objectId = now.getYear()+"/"+String.format("%02d", now.getMonthValue())+"/"+String.format("%02d", now.getDayOfMonth())+"/"+UUID.randomUUID()+"_"+fileName;
            Path target = baseDir.resolve(objectId).normalize();
            Files.createDirectories(target.getParent());
            long bytes = Files.copy(data, target);
            return new StoredObject(objectId, contentType, bytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    @Override
    public String getSignedReadUrl(String objectId, Duration ttl) {
        // For local dev, return a controller URL that will stream by objectId
        return "/api/v1/files/" + objectId;
    }

    @Override
    public void delete(String objectId) {
        try {
            Files.deleteIfExists(baseDir.resolve(objectId).normalize());
        } catch (IOException ignored) {}
    }

    public Path resolvePath(String objectId) {
        return baseDir.resolve(objectId).normalize();
    }
}


