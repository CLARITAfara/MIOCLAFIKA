package com.mixeo.deleter;

/** Programme 4 : supprime les fichiers MP3 reçus depuis la queue mp3.delete. */
public class Main {
    public static void main(String[] args) throws Exception {
        new DeleterService().start();
        System.out.println("Programme 4 (Deleter) démarré...");
        Thread.currentThread().join();
    }
}
