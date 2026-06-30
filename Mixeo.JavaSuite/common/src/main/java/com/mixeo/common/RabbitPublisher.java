package com.mixeo.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;

/** Equivalent de Mixeo.Common.RabbitPublisher (C#). */
public final class RabbitPublisher {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RabbitPublisher() {
    }

    public static void publishMessage(String queueName, String message) throws Exception {
        ConnectionFactory factory = RabbitConfig.createConnectionFactory();
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.queueDeclare(queueName, false, false, false, null);
            try {
                byte[] body = message.getBytes(StandardCharsets.UTF_8);
                channel.basicPublish("", queueName, null, body);
                FileLogger.log("RabbitMQ", "[RABBITMQ] Publish to '" + queueName + "': " + message);
            } catch (Exception ex) {
                FileLogger.log("RabbitMQ", "[RABBITMQ] Error publishing to '" + queueName + "': " + ex.getMessage());
                throw ex;
            }
        }
    }

    public static void publishJson(String queueName, Object obj) throws Exception {
        String json = MAPPER.writeValueAsString(obj);
        publishMessage(queueName, json);
    }
}
