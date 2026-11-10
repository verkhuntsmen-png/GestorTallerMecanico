package com.refaccionaria.auth.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Stores workshop photographs after validating their maximum size and binary signature. */
@Service
public class TallerPhotoStorageService {
    private static final long MAX_PHOTO_BYTES = 15L * 1024 * 1024;
    private final Path storageDirectory;

    public TallerPhotoStorageService(@Value("${app.storage.taller-photos-dir:uploads/taller-photos}") String storageDirectory) {
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile photo) {
        if (photo == null || photo.isEmpty() || photo.getSize() > MAX_PHOTO_BYTES) {
            throw new IllegalArgumentException("La fotografia es obligatoria y no puede superar 15 MB");
        }
        String extension = extensionOf(photo);
        String filename = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(storageDirectory);
            try (InputStream input = photo.getInputStream()) {
                Files.copy(input, storageDirectory.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            }
            return filename;
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible guardar la fotografia del taller", ex);
        }
    }

    public void deleteQuietly(String filename) {
        try { Files.deleteIfExists(storageDirectory.resolve(filename).normalize()); }
        catch (IOException ignored) { }
    }

    private String extensionOf(MultipartFile photo) {
        try (InputStream input = photo.getInputStream()) {
            byte[] header = input.readNBytes(12);
            if (header.length >= 3 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) return "jpg";
            if (header.length >= 8 && (header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47 && header[4] == 0x0D && header[5] == 0x0A && header[6] == 0x1A && header[7] == 0x0A) return "png";
            if (header.length >= 12 && header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46 && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50) return "webp";
        } catch (IOException ex) {
            throw new IllegalArgumentException("No fue posible leer la fotografia", ex);
        }
        throw new IllegalArgumentException("La fotografia debe ser JPEG, PNG o WEBP valido");
    }
}
