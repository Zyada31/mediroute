package com.mediroute.controller;

import com.mediroute.service.storage.LocalFsStorageService;
import jakarta.servlet.http.HttpServletResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.file.Files;

/**
 * StorageController serves stored evidence files from the pluggable StorageService.
 * Defaults to LocalFsStorageService in development.
 */
@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "Files", description = "Serve stored files via signed URLs or inline streaming")
public class StorageController {

	private final LocalFsStorageService storage;

	public StorageController(LocalFsStorageService storage) {
		this.storage = storage;
	}

	@GetMapping("/{objectId:.+}")
	@Operation(summary = "Stream a stored file (inline)")
	public void getFile(@PathVariable String objectId, HttpServletResponse res) throws IOException {
		var path = storage.resolvePath(objectId);
		if (!Files.exists(path)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
		}
		String ct = Files.probeContentType(path);
		res.setHeader(HttpHeaders.CONTENT_TYPE, ct != null ? ct : MediaType.APPLICATION_OCTET_STREAM_VALUE);
		res.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + path.getFileName() + "\"");
		Files.copy(path, res.getOutputStream());
		res.flushBuffer();
	}
}


