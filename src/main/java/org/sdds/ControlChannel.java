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
        } catch (JMSException | InterruptedException e) {
            LOG.error("Failed to publish control flow to topic: " + topic, e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
