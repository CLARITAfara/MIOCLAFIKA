package com.mixeo.common;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Equivalent de Mixeo.Common.FileLogger (C#). Ecrit dans logs/mixeo-app.log a la racine du repo. */
public final class FileLogger {

    private static final Object LOCK = new Object();
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Path LOG_DIR = resolveSharedLogDir();
    private static final Path LOG_FILE = LOG_DIR.resolve("mixeo-app.log");

    private FileLogger() {
    }

    /** Remonte les dossiers parents jusqu'a trouver "Mixeo.Common" / "Mixeo.JavaSuite", sinon ./logs. */
    private static Path resolveSharedLogDir() {
        File dir = new File(System.getProperty("user.dir"));
        while (dir != null) {
            if (new File(dir, "Mixeo.Common").exists()
                    || new File(dir, "Mixeo.JavaSuite").exists()
                    || new File(dir, ".git").exists()) {
                return dir.toPath().resolve("logs");
            }
            dir = dir.getParentFile();
        }
        return new File(System.getProperty("user.dir"), "logs").toPath();
    }

    public static void log(String module, String message) {
        try {
            synchronized (LOCK) {
                Files.createDirectories(LOG_DIR);
                String line = "[" + LocalDateTime.now().format(FORMAT) + "] [" + module + "] " + message
                        + System.lineSeparator();
                Files.writeString(LOG_FILE, line, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                System.out.print(line);
            }
        } catch (IOException ex) {
            System.err.println("Failed to write log: " + ex.getMessage());
        }
    }
}
