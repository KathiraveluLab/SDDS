package org.sdds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the Software-Defined Data Services (SDDS) framework.
 * Orchestrates the integration of Evora, Messaging4Transport, and SMART.
 */
public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        LOG.info("Initializing SDDS Framework (Mayan-DS)...");

        try {
            // STEP 1: Components Initialization
            ControlChannel control = new ControlChannel();
            Orchestrator orchestrator = new Orchestrator();
            ResilienceManager resilience = new ResilienceManager();

            // STEP 2: Start Dynamic Command Listener
            control.startCommandListener("sdds/commands", (command, payload) -> {
                switch (command.toUpperCase()) {
                    case "SCHEDULE":
                        orchestrator.scheduleService(payload);
                        break;
                    case "POLICY":
                        // Format: flowId,policy
                        String[] policyParts = payload.split(",");
                        if (policyParts.length == 2) {
                            resilience.applyPolicy(policyParts[0], policyParts[1]);
                        }
                        break;
                    default:
                        LOG.warn("Unknown command: {}", command);
                }
            });

            // STEP 3: Initial Demo Execution
            control.publishControlFlow("sdds/system", "INITIALIZING");
            orchestrator.scheduleService("inter-domain-sync");
            resilience.applyPolicy("flow-101", "REPLICATE");

            LOG.info("SDDS Framework (Mayan-DS) is now active and network-aware.");
            LOG.info("Use sdds_client.sh in another terminal to trigger actions.");

        } catch (Exception e) {
            LOG.error("Failed to initialize SDDS Framework", e);
        }
    }
}
