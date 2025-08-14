package com.mediroute.service.storage;

import java.io.InputStream;
import java.time.Duration;
import java.util.Map;

public interface StorageService {
    StoredObject store(InputStream data, String contentType, String suggestedName, Map<String,String> metadata);
    String getSignedReadUrl(String objectId, Duration ttl);
    void delete(String objectId);

    record StoredObject(String objectId, String contentType, long sizeBytes) {}
}


