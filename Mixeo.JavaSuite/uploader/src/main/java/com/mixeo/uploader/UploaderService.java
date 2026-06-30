package com.mixeo.uploader;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mixeo.common.FileLogger;
import com.mixeo.common.Mp3Metadata;
import com.mixeo.common.RabbitConfig;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Equivalent de Mixeo.Uploader.Services.UploaderService (C#). */
public class UploaderService {

    private static final String PROGRAM_NAME = "program3";
    private static final String UPLOAD_URL = "http://localhost:5021/api/mp3/upload";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper()
            // Accepte aussi bien les cles PascalCase (ancien C#) que camelCase (Java).
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public void start() throws Exception {
        ConnectionFactory factory = RabbitConfig.createConnectionFactory();
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(RabbitConfig.QUEUE_METADATA, false, false, false, null);

        FileLogger.log(PROGRAM_NAME, "Listening on queue: " + RabbitConfig.QUEUE_METADATA);
        System.out.println("👂 Listening queue: " + RabbitConfig.QUEUE_METADATA);

        DeliverCallback callback = (consumerTag, delivery) -> {
            String json = new String(delivery.getBody(), StandardCharsets.UTF_8);
            Mp3Metadata meta;
            try {
                meta = mapper.readValue(json, Mp3Metadata.class);
            } catch (Exception ex) {
                meta = null;
            }

            if (meta == null) {
                FileLogger.log(PROGRAM_NAME, "[RABBITMQ] Consume error: Received invalid message (null metadata).");
                FileLogger.log(PROGRAM_NAME, "[RABBITMQ] ACK message for invalid message.");
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                return;
            }

            FileLogger.log(PROGRAM_NAME, "[RABBITMQ] Consume metadata for: " + meta.getTitle() + " (File: " + meta.getPath() + ")");
            System.out.println("📦 Received: " + meta.getTitle());

            boolean ok = uploadToApi(meta);

            if (ok) {
                FileLogger.log(PROGRAM_NAME, "✅ Upload success: " + meta.getTitle());
                System.out.println("✅ Uploaded: " + meta.getTitle());
                try {
                    Path p = Path.of(meta.getPath());
                    if (Files.exists(p)) {
                        Files.delete(p);
                        FileLogger.log(PROGRAM_NAME, "🗑 Deleted original file: " + meta.getPath());
                        System.out.println("🗑 Deleted file: " + meta.getPath());
                    } else {
                        FileLogger.log(PROGRAM_NAME, "⚠️ File already gone: " + meta.getPath());
                    }
                } catch (Exception ex) {
                    FileLogger.log(PROGRAM_NAME, "❌ Delete error for " + meta.getPath() + ": " + ex.getMessage());
                    System.out.println("❌ Delete error: " + ex.getMessage());
                }
            } else {
                FileLogger.log(PROGRAM_NAME, "❌ Upload failed: " + meta.getTitle());
                System.out.println("❌ Upload failed: " + meta.getTitle());
            }

            FileLogger.log(PROGRAM_NAME, "[RABBITMQ] ACK message for: " + meta.getTitle());
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        };

        channel.basicConsume(RabbitConfig.QUEUE_METADATA, false, callback, consumerTag -> { });
    }

    private boolean uploadToApi(Mp3Metadata meta) {
        try {
            Path filePath = Path.of(meta.getPath());
            if (!Files.exists(filePath)) {
                FileLogger.log(PROGRAM_NAME, "Upload aborted: File not found at " + meta.getPath());
                return false;
            }

            String boundary = "----MixeoBoundary" + System.currentTimeMillis();
            byte[] body = buildMultipart(meta, filePath, boundary);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(UPLOAD_URL))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            FileLogger.log(PROGRAM_NAME, "API POST " + meta.getTitle() + " - Status: " + response.statusCode());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception ex) {
            FileLogger.log(PROGRAM_NAME, "Upload exception for " + meta.getTitle() + ": " + ex.getMessage());
            return false;
        }
    }

    /** Construit un corps multipart/form-data avec les champs texte + le fichier MP3. */
    private byte[] buildMultipart(Mp3Metadata meta, Path filePath, String boundary) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<String[]> fields = new ArrayList<>();
        fields.add(new String[]{"title", nullSafe(meta.getTitle())});
        fields.add(new String[]{"artist", nullSafe(meta.getArtist())});
        fields.add(new String[]{"album", nullSafe(meta.getAlbum())});
        fields.add(new String[]{"genre", nullSafe(meta.getGenre())});
        fields.add(new String[]{"year", String.valueOf(meta.getYear())});
        fields.add(new String[]{"duration", String.valueOf(meta.getDuration())});

        for (String[] field : fields) {
            out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Disposition: form-data; name=\"" + field[0] + "\"\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            out.write(field[1].getBytes(StandardCharsets.UTF_8));
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }

        String fileName = filePath.getFileName().toString();
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n")
                .getBytes(StandardCharsets.UTF_8));
        out.write("Content-Type: audio/mpeg\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        out.write(Files.readAllBytes(filePath));
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));

        out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
