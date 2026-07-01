package com.mixeo.common;

import com.rabbitmq.client.ConnectionFactory;

/** Equivalent de Mixeo.Common.RabbitConfig (C#). */
public final class RabbitConfig {

    public static final String HOST_NAME = "localhost";
    public static final int PORT = 5672;
    public static final String USER_NAME = "guest";
    public static final String PASSWORD = "guest";

    public static final String QUEUE_FILES    = "mp3.files";
    public static final String QUEUE_METADATA = "mp3.metadata";
    public static final String QUEUE_DELETE   = "mp3.delete";

    private RabbitConfig() {
    }

    public static ConnectionFactory createConnectionFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST_NAME);
        factory.setPort(PORT);
        factory.setUsername(USER_NAME);
        factory.setPassword(PASSWORD);
        return factory;
    }
}
