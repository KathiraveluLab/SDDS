package org.sdds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility client to send commands to a running SDDS instance via AMQP.
 */
public class ControlClient {
    private static final Logger LOG = LoggerFactory.getLogger(ControlClient.class);

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: ControlClient <COMMAND> <PAYLOAD>");
            System.out.println("Examples:");
            System.out.println("  ControlClient SCHEDULE compression");
            System.out.println("  ControlClient POLICY flow-102,CLONE");
            return;
        }

        String command = args[0];
        String payload = args[1];
        String fullMessage = command + ":" + payload;

        ControlChannel control = new ControlChannel();
        control.publishControlFlow("sdds/commands", fullMessage);
        
        System.out.println("Sent command: " + fullMessage);
        // Ensure the message is sent before exiting
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        System.exit(0);
    }
}
