package com.mixeo.deleter;

import com.mixeo.common.FileLogger;
import com.mixeo.common.RabbitConfig;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/** Programme 4 : consomme mp3.delete et supprime physiquement les fichiers. */
public class DeleterService {

    private static final String PROGRAM_NAME = "program4";

    public void start() throws Exception {
        ConnectionFactory factory = RabbitConfig.createConnectionFactory();
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(RabbitConfig.QUEUE_DELETE, false, false, false, null);

        FileLogger.log(PROGRAM_NAME, "Listening on queue: " + RabbitConfig.QUEUE_DELETE);
        System.out.println("👂 Programme 4 — Listening queue: " + RabbitConfig.QUEUE_DELETE);

        DeliverCallback callback = (consumerTag, delivery) -> {
            String filePath = new String(delivery.getBody(), StandardCharsets.UTF_8).trim();

            if (filePath.isBlank()) {
                FileLogger.log(PROGRAM_NAME, "[RABBITMQ] Message vide, ignoré.");
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                return;
            }

            FileLogger.log(PROGRAM_NAME, "[RABBITMQ] Reçu pour suppression : " + filePath);
            System.out.println("🗑 À supprimer : " + filePath);

            deleteFile(filePath);

            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        };

        channel.basicConsume(RabbitConfig.QUEUE_DELETE, false, callback, consumerTag -> { });
    }

    private void deleteFile(String filePath) {
        Path path = Path.of(filePath);
        try {
            Files.delete(path);
            FileLogger.log(PROGRAM_NAME, "✅ Supprimé : " + filePath);
            System.out.println("✅ Supprimé : " + filePath);
        } catch (NoSuchFileException ex) {
            FileLogger.log(PROGRAM_NAME, "⚠️ Fichier introuvable : " + filePath);
            System.out.println("⚠️ Introuvable : " + filePath);
        } catch (SecurityException ex) {
            FileLogger.log(PROGRAM_NAME, "❌ Accès refusé : " + filePath + " — " + ex.getMessage());
            System.out.println("❌ Accès refusé : " + filePath);
        } catch (Exception ex) {
            FileLogger.log(PROGRAM_NAME, "❌ Erreur suppression " + filePath + " : " + ex.getMessage());
            System.out.println("❌ Erreur : " + ex.getMessage());
        }
    }
}
