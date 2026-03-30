package org.sdds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.evora.core.Node;
import org.evora.core.UserPolicy;
import org.evora.core.PlacementSolution;
import java.util.HashMap;
import java.util.Map;

/**
 * SDDS Orchestrator handles service composition and execution placement.
 * Leverages the Évora research framework for SDN-enabled orchestration.
 */
public class Orchestrator {
    private static final Logger LOG = LoggerFactory.getLogger(Orchestrator.class);
    private org.evora.core.Orchestrator evoraOrchestrator;
    private Map<String, Node> nodeMap;

    public Orchestrator() {
        LOG.info("Initializing SDDS Orchestrator (Évora-backed)...");
        this.nodeMap = new HashMap<>();

        // Initialize nodes as defined in the Mayan-DS 12-node edge topology
        Node node1 = new Node("node-1", new String[] { "node-2" }, new String[] { "inter-domain-sync", "dedup" });
        Node node2 = new Node("node-2", new String[] { "node-1" }, new String[] { "inter-domain-sync", "compression" });

        nodeMap.put(node1.getId(), node1);
        nodeMap.put(node2.getId(), node2);

        this.evoraOrchestrator = new org.evora.core.Orchestrator(nodeMap);
    }

    /**
     * Schedules a data service based on distance minimization (Equation 1).
     * 
     * @param serviceId The ID of the service to schedule.
     */
    public void scheduleService(String serviceId) {
        LOG.info("Scheduling Data Service: {} using network-aware placement...", serviceId);

        String[] serviceChain = { serviceId };
        // alpha=0.5, beta=0.3, gamma=0.2 (Cost, Latency, Throughput weights)
        UserPolicy policy = new UserPolicy(0.5, 0.3, 0.2);

        // Invoke Évora composition logic
        PlacementSolution solution = evoraOrchestrator.solveGreedy(serviceChain, policy);

        if (solution.isCompliant()) {
            LOG.info("Service {} successfully scheduled to node: {}",
                    serviceId, solution.getMappings().get(serviceId));
        } else {
            LOG.warn("No compliant placement found for service: {}", serviceId);
        }
    }
}
