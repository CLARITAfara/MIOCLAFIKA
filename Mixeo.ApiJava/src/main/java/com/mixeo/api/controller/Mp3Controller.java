package com.mixeo.api.controller;

import com.mixeo.api.model.Mp3File;
import com.mixeo.api.repository.Mp3FileRepository;
import com.mixeo.api.service.LyricsService;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Equivalent de Mixeo.Api.Controllers.Mp3Controller (C#).
 * Route : /api/mp3.
 */
@RestController
@RequestMapping("/api/mp3")
public class Mp3Controller {

    private final Mp3FileRepository mp3Files;
    private final LyricsService lyricsService;

    public Mp3Controller(Mp3FileRepository mp3Files, LyricsService lyricsService) {
        this.mp3Files = mp3Files;
        this.lyricsService = lyricsService;
        // jaudiotagger est tres bavard par defaut : on reduit ses logs.
        Logger.getLogger("org.jaudiotagger").setLevel(Level.OFF);
    }

    // 1. UPLOAD
    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "artist", required = false) String artist,
            @RequestParam(value = "album", required = false) String album,
            @RequestParam(value = "genre", required = false) String genre,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "duration", required = false) Integer duration,
            @RequestParam(value = "useMetadata", defaultValue = "false") boolean useMetadata,
            @RequestParam(value = "file", required = false) MultipartFile file) throws Exception {

        String filePath = null;

        if (file != null && !file.isEmpty()) {
            Path folder = Paths.get("Uploads", "mp3");
            Files.createDirectories(folder);

            Path target = folder.resolve(UUID.randomUUID() + "_" + file.getOriginalFilename());
            file.transferTo(target.toAbsolutePath().toFile());
            filePath = target.toString();
        }

        if (useMetadata && filePath != null) {
            try {
                AudioFile audio = AudioFileIO.read(new File(filePath));
                Tag tag = audio.getTag();
                if (tag != null) {
                    title = orKeep(tag.getFirst(FieldKey.TITLE), title);
                    artist = orKeep(tag.getFirst(FieldKey.ARTIST), artist);
                    album = orKeep(tag.getFirst(FieldKey.ALBUM), album);
                    genre = orKeep(tag.getFirst(FieldKey.GENRE), genre);
                    Integer parsedYear = parseIntOrNull(tag.getFirst(FieldKey.YEAR));
                    if (parsedYear != null && parsedYear > 0) year = parsedYear;
                }
                int len = audio.getAudioHeader().getTrackLength();
                if (len > 0) duration = len;
            } catch (Exception ignored) {
                // On ignore les erreurs de lecture des tags, comme le catch C#.
            }
        }

        Mp3File mp3 = new Mp3File();
        String fallbackTitle = (file != null) ? stripExtension(file.getOriginalFilename()) : "Unknown";
        mp3.setTitle(isBlank(title) ? (fallbackTitle == null ? "Unknown" : fallbackTitle) : title);
        mp3.setArtist(artist);
        mp3.setAlbum(album);
        mp3.setGenre(genre);
        mp3.setLanguage(language);
        mp3.setYear(year == null ? 0 : year);
        mp3.setDuration(duration == null ? 0 : duration);
        mp3.setFilePath(filePath);
        mp3.setCreatedAt(LocalDateTime.now());

        mp3Files.save(mp3);
        return ResponseEntity.ok(mp3);
    }

    // 2. GET ALL
    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(mp3Files.findAllByOrderByCreatedAtDesc());
    }

    // 3. GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable int id) {
        return mp3Files.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 4. UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody Mp3File updated) {
        Optional<Mp3File> existing = mp3Files.findById(id);
        if (existing.isEmpty()) return ResponseEntity.notFound().build();

        Mp3File mp3 = existing.get();
        mp3.setTitle(updated.getTitle());
        mp3.setArtist(updated.getArtist());
        mp3.setAlbum(updated.getAlbum());
        mp3.setGenre(updated.getGenre());
        mp3.setLanguage(updated.getLanguage());
        mp3.setYear(updated.getYear());
        mp3.setDuration(updated.getDuration());

        mp3Files.save(mp3);
        return ResponseEntity.ok(mp3);
    }

    // 5. DELETE (+ suppression du fichier physique)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable int id) {
        Optional<Mp3File> existing = mp3Files.findById(id);
        if (existing.isEmpty()) return ResponseEntity.notFound().build();

        Mp3File mp3 = existing.get();
        if (mp3.getFilePath() != null && !mp3.getFilePath().isEmpty()) {
            File f = new File(mp3.getFilePath());
            if (f.exists()) {
                f.delete();
            }
        }

        mp3Files.delete(mp3);
        return ResponseEntity.ok(Map.of("message", "File deleted successfully"));
    }

    // 6. GET LYRICS
    @GetMapping("/{id}/lyrics")
    public ResponseEntity<?> getLyrics(@PathVariable int id) {
        Optional<Mp3File> existing = mp3Files.findById(id);
        if (existing.isEmpty()) return ResponseEntity.status(404).body("Chanson introuvable.");

        String lyrics = lyricsService.getOrDownloadLyrics(existing.get());
        if (lyrics == null || lyrics.isEmpty()) {
            return ResponseEntity.status(404).body("Paroles indisponibles.");
        }
        return ResponseEntity.ok(Map.of("text", lyrics));
    }

    // ─── Helpers ───
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String orKeep(String fromTag, String current) {
        return isBlank(fromTag) ? current : fromTag;
    }

    private static String stripExtension(String fileName) {
        if (fileName == null) return null;
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static Integer parseIntOrNull(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            // certains tags YEAR contiennent une date complete : on garde les 4 premiers chiffres
            String trimmed = s.trim();
            if (trimmed.length() >= 4) trimmed = trimmed.substring(0, 4);
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
