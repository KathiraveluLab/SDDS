package org.sdds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.messaging4transport.impl.AmqpPublisher;
import org.opendaylight.messaging4transport.impl.AmqpConfig;
import org.apache.qpid.amqp_1_0.jms.impl.ConnectionFactoryImpl;
import javax.jms.*;
import java.util.function.BiConsumer;

/**
 * SDDS Control Channel manages the asynchronous communication between planes.
 * Leverages Messaging4Transport for AMQP-based MD-SAL bindings.
 */
public class ControlChannel {
    private static final Logger LOG = LoggerFactory.getLogger(ControlChannel.class);

    public ControlChannel() {
        LOG.info("Initializing SDDS Control Channel (M4T-backed)...");
    }

    /**
     * Publishes a control message via AMQP.
     * @param topic The AMQP topic to publish to.
     * @param message The message content.
     */
    public void publishControlFlow(String topic, String message) {
        LOG.info("Publishing Control Flow to [{}]: {}", topic, message);
        try {
            // Updated to use the correct class name for the 1.0.4-Beryllium-SR4 version
            AmqpPublisher.publish(topic, message);
            LOG.debug("Successfully published message to topic: {}", topic);
        } catch (JMSException e) {
            if (e.getCause() instanceof java.net.ConnectException || e.getMessage().contains("Connection refused")) {
                LOG.warn("Could not connect to AMQP Broker on localhost:5672. Ensure the broker is running.");
            } else {
                LOG.error("Failed to publish control flow to topic: " + topic, e);
            }
        } catch (InterruptedException e) {
            LOG.error("Publishing interrupted for topic: " + topic, e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Starts an asynchronous listener for SDDS commands.
     * @param topic The topic to listen on.
     * @param commandHandler callback for (command, payload)
     */
    public void startCommandListener(String topic, BiConsumer<String, String> commandHandler) {
        new Thread(() -> {
            try {
                String user = AmqpConfig.getUser();
                String password = AmqpConfig.getPassword();
                ConnectionFactoryImpl factory = new ConnectionFactoryImpl(AmqpConfig.getHost(), AmqpConfig.getPort(), user, password);

                Connection connection = factory.createConnection(user, password);
                connection.start();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                MessageConsumer consumer = session.createConsumer(AmqpConfig.getDestination(topic));

                LOG.info("Control Channel Listener active on topic: {}", topic);

                while (true) {
                    Message msg = consumer.receive();
                    if (msg instanceof TextMessage) {
                        String body = ((TextMessage) msg).getText();
                        LOG.info("Received External Command: {}", body);
                        
                        // Parse command format: "COMMAND:PAYLOAD"
                        String[] parts = body.split(":", 2);
                        if (parts.length == 2) {
                            commandHandler.accept(parts[0].trim(), parts[1].trim());
                        } else {
                            LOG.warn("Invalid command format received: {}", body);
                        }
                    }
                }
            } catch (JMSException e) {
                LOG.error("Control Channel Listener failed", e);
            }
        }).start();
    }
}
