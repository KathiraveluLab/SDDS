package org.sdds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.messaging4transport.impl.AmqpPublisher;
import javax.jms.JMSException;

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
}
