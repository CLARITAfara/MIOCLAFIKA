package com.mixeo.desktop.service;

import com.mixeo.common.Mp3Metadata;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Equivalent de Mixeo.Desktop.Services.MetadataService (C#), via jaudiotagger au lieu de TagLib. */
public class MetadataService {

    public MetadataService() {
        Logger.getLogger("org.jaudiotagger").setLevel(Level.OFF);
    }

    public Mp3Metadata extract(String filePath) throws Exception {
        AudioFile audio = AudioFileIO.read(new File(filePath));
        Tag tag = audio.getTag();

        Mp3Metadata meta = new Mp3Metadata();
        meta.setPath(filePath);
        if (tag != null) {
            meta.setTitle(orEmpty(tag.getFirst(FieldKey.TITLE)));
            meta.setAlbum(orEmpty(tag.getFirst(FieldKey.ALBUM)));
            meta.setGenre(orEmpty(tag.getFirst(FieldKey.GENRE)));
            meta.setArtist(orEmpty(tag.getFirst(FieldKey.ARTIST)));
            meta.setYear(parseYear(tag.getFirst(FieldKey.YEAR)));
        }
        meta.setDuration(audio.getAudioHeader().getTrackLength());
        return meta;
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }

    private static int parseYear(String s) {
        if (s == null || s.trim().isEmpty()) return 0;
        try {
            String t = s.trim();
            if (t.length() >= 4) t = t.substring(0, 4);
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
