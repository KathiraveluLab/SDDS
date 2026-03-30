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
            // STEP 1: Control Plane (Messaging4Transport)
            ControlChannel control = new ControlChannel();
            control.publishControlFlow("sdds/system", "INITIALIZING");

            // STEP 2: Orchestration Plane (Evora)
            Orchestrator orchestrator = new Orchestrator();
            orchestrator.scheduleService("inter-domain-sync");

            // STEP 3: Resilience Plane (SMART)
            ResilienceManager resilience = new ResilienceManager();
            resilience.applyPolicy("flow-101", "REPLICATE");

            LOG.info("SDDS Framework (Mayan-DS) is now active and network-aware.");

        } catch (Exception e) {
            LOG.error("Failed to initialize SDDS Framework", e);
        }
    }
}
