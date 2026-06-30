package com.mixeo.api.service;

import com.mixeo.api.model.Mp3File;
import com.mixeo.api.repository.Mp3FileRepository;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

/**
 * Equivalent de Mixeo.Api.Services.LyricsService (C#).
 * Recupere les paroles depuis la base, sinon appelle le script Python Mixeo.Lyric/main.py.
 */
@Service
public class LyricsService {

    private final Mp3FileRepository mp3Files;

    public LyricsService(Mp3FileRepository mp3Files) {
        this.mp3Files = mp3Files;
    }

    public String getOrDownloadLyrics(Mp3File mp3) {
        // 1. Deja en base ?
        if (mp3.getLyrics() != null && !mp3.getLyrics().isEmpty()) {
            return mp3.getLyrics();
        }

        // 2. Construction de la requete
        String title = mp3.getTitle() == null ? "" : mp3.getTitle();
        String artist = mp3.getArtist() == null ? "" : mp3.getArtist();
        String songQuery = (title + " " + artist).trim();
        if (songQuery.isEmpty()) {
            return null;
        }

        // 3. Resolution du chemin du script Python (relatif a la racine du repo).
        File script = Paths.get("..", "Mixeo.Lyric", "main.py").toFile();
        if (!script.exists()) {
            return null;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder("python", script.getAbsolutePath(), songQuery);
            pb.directory(script.getParentFile());
            pb.redirectErrorStream(false);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }
            process.waitFor();

            String result = output.toString().trim();
            if (!result.isEmpty()) {
                mp3.setLyrics(result);
                mp3Files.save(mp3);
                return result;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
