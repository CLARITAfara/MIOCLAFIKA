package com.mixeo.api.controller;

import com.mixeo.api.dto.MergePlaylistDto;
import com.mixeo.api.dto.PlaylistCriteriaDto;
import com.mixeo.api.dto.ReplaceTrackDto;
import com.mixeo.api.dto.SavePlaylistDto;
import com.mixeo.api.dto.TrackActionDto;
import com.mixeo.api.model.Mp3File;
import com.mixeo.api.model.Playlist;
import com.mixeo.api.model.PlaylistRule;
import com.mixeo.api.model.PlaylistTrack;
import com.mixeo.api.repository.Mp3FileRepository;
import com.mixeo.api.repository.PlaylistRepository;
import com.mixeo.api.repository.PlaylistRuleRepository;
import com.mixeo.api.repository.PlaylistTrackRepository;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Equivalent de Mixeo.Api.Controllers.PlaylistController (C#).
 * Route : /api/playlists.
 */
@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {

    private final Mp3FileRepository mp3Files;
    private final PlaylistRepository playlists;
    private final PlaylistTrackRepository playlistTracks;
    private final PlaylistRuleRepository playlistRules;

    public PlaylistController(Mp3FileRepository mp3Files,
                             PlaylistRepository playlists,
                             PlaylistTrackRepository playlistTracks,
                             PlaylistRuleRepository playlistRules) {
        this.mp3Files = mp3Files;
        this.playlists = playlists;
        this.playlistTracks = playlistTracks;
        this.playlistRules = playlistRules;
    }

    // 1. GENERATION TEMPORAIRE
    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestBody PlaylistCriteriaDto criteria) {
        List<Mp3File> all = mp3Files.findAll();

        // PRIORITE 1 : LES EXCLUSIONS
        List<Mp3File> filtered = all.stream()
                .filter(m -> !excluded(m.getGenre(), criteria.getExcludeGenres()))
                .filter(m -> !excluded(m.getArtist(), criteria.getExcludeArtists()))
                .filter(m -> !excluded(m.getAlbum(), criteria.getExcludeAlbums()))
                .collect(Collectors.toList());

        // PRIORITE 2 : LES INCLUSIONS CIBLEES
        boolean hasInclusions = false;
        if (notEmpty(criteria.getLanguages())) {
            hasInclusions = true;
            filtered = filtered.stream().filter(m -> included(m.getLanguage(), criteria.getLanguages())).collect(Collectors.toList());
        }
        if (notEmpty(criteria.getGenres())) {
            hasInclusions = true;
            filtered = filtered.stream().filter(m -> included(m.getGenre(), criteria.getGenres())).collect(Collectors.toList());
        }
        if (notEmpty(criteria.getArtists())) {
            hasInclusions = true;
            filtered = filtered.stream().filter(m -> included(m.getArtist(), criteria.getArtists())).collect(Collectors.toList());
        }
        if (notEmpty(criteria.getAlbums())) {
            hasInclusions = true;
            filtered = filtered.stream().filter(m -> included(m.getAlbum(), criteria.getAlbums())).collect(Collectors.toList());
        }

        List<Mp3File> availableTracks = new ArrayList<>(filtered);

        // Securite : si critere specifique mais aucun resultat, on rejoue uniquement les exclusions
        // (genres + artistes, comme l'API C#) sur la base complete.
        if (availableTracks.isEmpty() && hasInclusions) {
            availableTracks = all.stream()
                    .filter(m -> !excluded(m.getGenre(), criteria.getExcludeGenres()))
                    .filter(m -> !excluded(m.getArtist(), criteria.getExcludeArtists()))
                    .collect(Collectors.toList());
        }

        // PRIORITE 3 : RANDOMISATION
        Collections.shuffle(availableTracks, new Random());

        // PRIORITE 4 : CONTROLE DE LA DUREE MAX avec marge de +59 s
        List<Mp3File> selected = new ArrayList<>();
        int currentDuration = 0;
        int lowerBound = criteria.getTotalDuration();
        int upperBound = criteria.getTotalDuration() + 59;

        for (Mp3File track : availableTracks) {
            int trackDuration = track.getDuration() == null ? 0 : track.getDuration();
            if (currentDuration + trackDuration <= upperBound) {
                selected.add(track);
                currentDuration += trackDuration;
                if (currentDuration >= lowerBound) break;
            }
        }

        // Completer avec le plus petit morceau restant si on est sous la borne basse
        if (currentDuration < lowerBound) {
            Mp3File remaining = availableTracks.stream()
                    .filter(t -> !selected.contains(t))
                    .min(Comparator.comparingInt(t -> t.getDuration() == null ? Integer.MAX_VALUE : t.getDuration()))
                    .orElse(null);
            if (remaining != null) {
                int rd = remaining.getDuration() == null ? 0 : remaining.getDuration();
                if (currentDuration + rd <= upperBound) {
                    selected.add(remaining);
                    currentDuration += rd;
                }
            }
        }

        // Fallback : ne jamais renvoyer une liste vide
        if (selected.isEmpty() && !availableTracks.isEmpty()) {
            Mp3File first = availableTracks.get(0);
            selected.add(first);
            currentDuration = first.getDuration() == null ? 0 : first.getDuration();
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tracks", selected);
        body.put("totalDuration", currentDuration);
        return ResponseEntity.ok(body);
    }

    // 2. SAUVEGARDE
    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody SavePlaylistDto dto) {
        if (dto.getMp3Ids() == null || dto.getMp3Ids().isEmpty()) {
            return ResponseEntity.badRequest().body("Cannot save an empty playlist.");
        }

        List<Mp3File> tracks = mp3Files.findAllById(dto.getMp3Ids());
        int totalDuration = tracks.stream().mapToInt(t -> t.getDuration() == null ? 0 : t.getDuration()).sum();

        Playlist playlist = new Playlist();
        playlist.setName(dto.getName());
        playlist.setTotalDuration(totalDuration);
        playlist.setUserId(dto.getUserId());
        playlist.setCreatedAt(LocalDateTime.now());
        playlists.saveAndFlush(playlist);

        for (Integer trackId : dto.getMp3Ids()) {
            PlaylistTrack pt = new PlaylistTrack();
            pt.setPlaylistId(playlist.getId());
            pt.setMp3Id(trackId);
            playlistTracks.save(pt);
        }

        if (dto.getCriteria() != null) {
            PlaylistCriteriaDto c = dto.getCriteria();
            PlaylistRule rule = new PlaylistRule();
            rule.setPlaylistId(playlist.getId());
            rule.setMaxDuration(c.getTotalDuration());
            rule.setGenre(String.join(",", c.getGenres()));
            rule.setArtist(String.join(",", c.getArtists()));
            rule.setLanguage(String.join(",", c.getLanguages()));
            rule.setExcludeArtist(String.join(",", c.getExcludeArtists()));
            rule.setExcludeGenre(String.join(",", c.getExcludeGenres()));
            playlistRules.save(rule);
        }

        return ResponseEntity.ok(reload(playlist.getId()));
    }

    // 2.5 FUSION DE PLAYLISTS
    @PostMapping("/merge")
    public ResponseEntity<?> merge(@RequestBody MergePlaylistDto dto) {
        if (dto.getPlaylistIds() == null || dto.getPlaylistIds().size() < 2) {
            return ResponseEntity.badRequest().body("Sélectionnez au moins 2 playlists à fusionner.");
        }
        if (dto.getNewName() == null || dto.getNewName().isBlank()) {
            return ResponseEntity.badRequest().body("Le nom de la nouvelle playlist est requis.");
        }

        Set<Integer> mp3IdSet = new LinkedHashSet<>();
        for (Integer playlistId : dto.getPlaylistIds()) {
            Optional<Playlist> p = playlists.findById(playlistId);
            if (p.isEmpty()) continue;
            playlistTracks.findByPlaylistId(playlistId).forEach(t -> mp3IdSet.add(t.getMp3Id()));
        }

        if (mp3IdSet.isEmpty()) {
            return ResponseEntity.badRequest().body("Aucun morceau trouvé dans les playlists sélectionnées.");
        }

        List<Mp3File> tracks = mp3Files.findAllById(mp3IdSet);
        int totalDuration = tracks.stream().mapToInt(t -> t.getDuration() == null ? 0 : t.getDuration()).sum();

        Playlist merged = new Playlist();
        merged.setName(dto.getNewName().trim());
        merged.setTotalDuration(totalDuration);
        merged.setUserId(dto.getUserId());
        merged.setCreatedAt(LocalDateTime.now());
        playlists.saveAndFlush(merged);

        for (Integer mp3Id : mp3IdSet) {
            PlaylistTrack pt = new PlaylistTrack();
            pt.setPlaylistId(merged.getId());
            pt.setMp3Id(mp3Id);
            playlistTracks.save(pt);
        }

        return ResponseEntity.ok(reload(merged.getId()));
    }

    // 3. GET PLAYLISTS BY USER ID
    @GetMapping
    public ResponseEntity<?> getPlaylists(@RequestParam(value = "userId", required = false) Integer userId) {
        List<Playlist> result = (userId != null) ? playlists.findByUserId(userId) : playlists.findAll();
        return ResponseEntity.ok(result);
    }

    // 3.5 DELETE PLAYLIST
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlaylist(@PathVariable int id) {
        Optional<Playlist> existing = playlists.findById(id);
        if (existing.isEmpty()) return ResponseEntity.notFound().build();
        playlists.delete(existing.get());
        return ResponseEntity.ok(Map.of("success", true));
    }

    // 3.6 ADD TRACK
    @PostMapping("/{id}/add-track")
    public ResponseEntity<?> addTrack(@PathVariable int id, @RequestBody TrackActionDto dto) {
        Optional<Playlist> existing = playlists.findById(id);
        if (existing.isEmpty()) return ResponseEntity.status(404).body("Playlist not found");
        if (mp3Files.findById(dto.getTrackId()).isEmpty()) return ResponseEntity.status(404).body("Track not found");

        PlaylistTrack pt = new PlaylistTrack();
        pt.setPlaylistId(id);
        pt.setMp3Id(dto.getTrackId());
        playlistTracks.save(pt);

        recalculateDuration(existing.get());
        return ResponseEntity.ok(reload(id));
    }

    // 3.7 REMOVE TRACK
    @PostMapping("/{id}/remove-track")
    public ResponseEntity<?> removeTrack(@PathVariable int id, @RequestBody TrackActionDto dto) {
        Optional<Playlist> existing = playlists.findById(id);
        if (existing.isEmpty()) return ResponseEntity.status(404).body("Playlist not found");

        playlistTracks.findFirstByPlaylistIdAndMp3Id(id, dto.getTrackId())
                .ifPresent(playlistTracks::delete);

        recalculateDuration(existing.get());
        return ResponseEntity.ok(reload(id));
    }

    // 3.8 REPLACE TRACK
    @PostMapping("/{id}/replace-track")
    public ResponseEntity<?> replaceTrack(@PathVariable int id, @RequestBody ReplaceTrackDto dto) {
        Optional<Playlist> existing = playlists.findById(id);
        if (existing.isEmpty()) return ResponseEntity.status(404).body("Playlist not found");

        playlistTracks.findFirstByPlaylistIdAndMp3Id(id, dto.getOldTrackId())
                .ifPresent(playlistTracks::delete);

        PlaylistTrack pt = new PlaylistTrack();
        pt.setPlaylistId(id);
        pt.setMp3Id(dto.getNewTrackId());
        playlistTracks.save(pt);

        recalculateDuration(existing.get());
        return ResponseEntity.ok(reload(id));
    }

    // 4. STREAMING D'UN MP3 (support des Range pour la lecture/seek du lecteur audio)
    @GetMapping("/stream/{mp3Id}")
    public ResponseEntity<StreamingResponseBody> stream(@PathVariable int mp3Id,
                                                        @RequestHeader HttpHeaders headers) {
        Optional<Mp3File> existing = mp3Files.findById(mp3Id);
        if (existing.isEmpty() || existing.get().getFilePath() == null) {
            return ResponseEntity.notFound().build();
        }
        File file = new File(existing.get().getFilePath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        long fileLength = file.length();
        long start = 0;
        long end = fileLength - 1;
        HttpStatus status = HttpStatus.OK;

        List<HttpRange> ranges = headers.getRange();
        if (!ranges.isEmpty()) {
            HttpRange range = ranges.get(0);
            start = range.getRangeStart(fileLength);
            end = range.getRangeEnd(fileLength);
            status = HttpStatus.PARTIAL_CONTENT;
        }

        long contentLength = end - start + 1;
        final long copyStart = start;
        final long copyLength = contentLength;

        StreamingResponseBody body = out -> {
            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                raf.seek(copyStart);
                byte[] buffer = new byte[8192];
                long remaining = copyLength;
                int read;
                while (remaining > 0
                        && (read = raf.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                    out.write(buffer, 0, read);
                    remaining -= read;
                }
            }
        };

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.ACCEPT_RANGES, "bytes");
        responseHeaders.setContentType(MediaType.parseMediaType("audio/mpeg"));
        responseHeaders.setContentLength(contentLength);
        if (status == HttpStatus.PARTIAL_CONTENT) {
            responseHeaders.add(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLength);
        }

        return new ResponseEntity<>(body, responseHeaders, status);
    }

    // 5. TELECHARGEMENT ZIP
    @GetMapping("/{id}/download-zip")
    public ResponseEntity<?> downloadZip(@PathVariable int id) throws Exception {
        Optional<Playlist> existing = playlists.findById(id);
        if (existing.isEmpty()) return ResponseEntity.status(404).body("Playlist not found");
        Playlist playlist = existing.get();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StringBuilder metadata = new StringBuilder();
        metadata.append("Playlist Name: ").append(playlist.getName()).append("\n");
        metadata.append("Total Duration: ").append(playlist.getTotalDuration()).append("\n");
        metadata.append("Created At: ").append(playlist.getCreatedAt()).append("\n");
        metadata.append("--------------------------------\n");

        Set<String> usedNames = new HashSet<>();
        int index = 1;

        try (ZipOutputStream zip = new ZipOutputStream(baos)) {
            for (PlaylistTrack track : playlist.getTracks()) {
                Mp3File file = track.getMp3File();
                if (file == null) continue;

                metadata.append(index).append(". ").append(file.getArtist()).append(" - ").append(file.getTitle()).append("\n");
                index++;

                if (file.getFilePath() == null || file.getFilePath().isBlank() || !new File(file.getFilePath()).exists()) {
                    continue;
                }

                String extension = getExtension(file.getFilePath());
                String entryName = file.getArtist() + " - " + file.getTitle() + extension;
                int duplicate = 1;
                while (usedNames.contains(entryName)) {
                    entryName = file.getArtist() + " - " + file.getTitle() + " (" + duplicate + ")" + extension;
                    duplicate++;
                }
                usedNames.add(entryName);

                zip.putNextEntry(new ZipEntry(entryName));
                try (InputStream in = Files.newInputStream(new File(file.getFilePath()).toPath())) {
                    in.transferTo(zip);
                }
                zip.closeEntry();
            }

            zip.putNextEntry(new ZipEntry("metadata.txt"));
            zip.write(metadata.toString().getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + playlist.getName() + ".zip\"")
                .body(baos.toByteArray());
    }

    // ─── Helpers ───

    private Playlist reload(int id) {
        return playlists.findById(id).orElse(null);
    }

    private void recalculateDuration(Playlist playlist) {
        List<PlaylistTrack> tracks = playlistTracks.findByPlaylistId(playlist.getId());
        int total = tracks.stream()
                .mapToInt(t -> t.getMp3File() != null && t.getMp3File().getDuration() != null
                        ? t.getMp3File().getDuration() : 0)
                .sum();
        playlist.setTotalDuration(total);
        playlists.save(playlist);
    }

    private static boolean notEmpty(List<String> list) {
        return list != null && !list.isEmpty();
    }

    /** true si la valeur du morceau figure dans la liste d'exclusion (insensible a la casse). */
    private static boolean excluded(String value, List<String> excludeList) {
        if (!notEmpty(excludeList)) return false;
        if (value == null) return false; // valeur null => non exclue (comme "Genre == null || ..." en C#)
        String lower = value.toLowerCase();
        return excludeList.stream().map(String::toLowerCase).anyMatch(lower::equals);
    }

    /** true si la valeur du morceau figure dans la liste autorisee (insensible a la casse). */
    private static boolean included(String value, List<String> includeList) {
        if (value == null) return false;
        String lower = value.toLowerCase();
        return includeList.stream().map(String::toLowerCase).anyMatch(lower::equals);
    }

    private static String getExtension(String path) {
        int dot = path.lastIndexOf('.');
        return dot >= 0 ? path.substring(dot) : "";
    }
}
