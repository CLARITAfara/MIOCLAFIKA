package com.mixeo.desktop.service;

import com.mixeo.common.FileLogger;
import com.mixeo.common.Mp3Metadata;
import com.mixeo.desktop.model.Mp3FileItem;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Equivalent de Mixeo.Desktop.Services.FolderWatcherService (C#).
 * Scanne un dossier, applique la blacklist (artistes/genres) et supprime les fichiers bannis.
 */
public class FolderWatcherService {

    private final MetadataService metadataService = new MetadataService();

    public List<Mp3FileItem> scanFolder(String folder) {
        List<Mp3FileItem> validFiles = new ArrayList<>();

        File dir = new File(folder);
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".mp3"));
        if (files == null) {
            return validFiles;
        }

        List<String> artistBlacklist = loadBlacklist("bl_artist.csv");
        List<String> genreBlacklist = loadBlacklist("bl_genre.csv");

        for (File file : files) {
            try {
                Mp3Metadata meta = metadataService.extract(file.getAbsolutePath());
                boolean isBlacklisted = false;

                if (meta.getArtist() != null && !meta.getArtist().isEmpty()) {
                    String artistLower = meta.getArtist().toLowerCase();
                    isBlacklisted = artistBlacklist.stream().anyMatch(b -> artistLower.contains(b.toLowerCase()));
                }

                if (!isBlacklisted && meta.getGenre() != null && !meta.getGenre().isEmpty()) {
                    String genreLower = meta.getGenre().toLowerCase();
                    isBlacklisted = genreBlacklist.stream().anyMatch(b -> genreLower.contains(b.toLowerCase()));
                }

                if (isBlacklisted) {
                    String banned = !meta.getArtist().isEmpty() ? meta.getArtist() : meta.getGenre();
                    FileLogger.log("program1",
                            "[BLACKLIST] Suppression du fichier : " + file.getAbsolutePath() + " (Artiste/Genre banni: " + banned + ")");
                    file.delete();
                } else {
                    // Les metadonnees sont deja extraites : on enrichit la ligne de la grille.
                    Mp3FileItem item = toItem(file);
                    item.setArtist(meta.getArtist());
                    item.setAlbum(meta.getAlbum());
                    item.setGenre(meta.getGenre());
                    item.setYear(meta.getYear());
                    item.setDuration(meta.getDuration());
                    validFiles.add(item);
                }
            } catch (Exception ex) {
                FileLogger.log("program1",
                        "[ERROR] Impossible de traiter le fichier " + file.getAbsolutePath() + " : " + ex.getMessage());
                // On l'ajoute quand meme si la lecture des metadonnees echoue (comme le C#).
                validFiles.add(toItem(file));
            }
        }

        return validFiles;
    }

    private static Mp3FileItem toItem(File file) {
        Mp3FileItem item = new Mp3FileItem();
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        item.setTitle(dot > 0 ? name.substring(0, dot) : name);
        item.setAbsolutePath(file.getAbsolutePath());
        return item;
    }

    /**
     * Charge une blacklist CSV (valeurs separees par des virgules).
     * Cherche d'abord blacklist/<name> en remontant les dossiers parents,
     * sinon retombe sur l'ancien chemin absolu du projet C#.
     */
    private static List<String> loadBlacklist(String csvName) {
        File csv = resolveBlacklistFile(csvName);
        if (csv == null || !csv.exists()) {
            return new ArrayList<>();
        }
        try {
            String text = Files.readString(csv.toPath(), StandardCharsets.UTF_8);
            return Arrays.stream(text.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    private static File resolveBlacklistFile(String csvName) {
        File dir = new File(System.getProperty("user.dir"));
        while (dir != null) {
            File candidate = new File(dir, "blacklist" + File.separator + csvName);
            if (candidate.exists()) {
                return candidate;
            }
            dir = dir.getParentFile();
        }
        // Repli : ancien chemin absolu du projet C#.
        return new File("d:\\L3\\GProjet\\Mixeo\\blacklist\\" + csvName);
    }
}
