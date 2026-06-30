package com.mixeo.uploader;

/** Equivalent du Program.cs de Mixeo.Uploader (C#). */
public class Main {
    public static void main(String[] args) throws Exception {
        UploaderService service = new UploaderService();
        service.start();

        System.out.println("Uploader started...");
        // Maintient le process vivant pour continuer a consommer la queue.
        Thread.currentThread().join();
    }
}
