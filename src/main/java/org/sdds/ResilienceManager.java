package org.sdds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Optional;
import java.util.Random;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;

/**
 * SDDS Resilience Manager handles QoS policies and selective redundancy.
 * Leverages the SMART research framework for differentiated QoS in multi-tenant clouds.
 */
public class ResilienceManager {
    private static final Logger LOG = LoggerFactory.getLogger(ResilienceManager.class);
    private final Random random = new Random();
    private final DOMDataBroker domDataBroker;

    // QNames for OpenDaylight Inventory and Port Statistics (Research Parity with SMART)
    private static final QName INVENTORY_NODES = QName.create("urn:opendaylight:inventory", "2013-08-19", "nodes");
    private static final QName INVENTORY_NODE = QName.create("urn:opendaylight:inventory", "2013-08-19", "node");
    private static final QName INVENTORY_NODE_ID = QName.create("urn:opendaylight:inventory", "2013-08-19", "id");
    private static final QName INVENTORY_NODE_CONNECTOR = QName.create("urn:opendaylight:inventory", "2013-08-19", "node-connector");
    private static final QName INVENTORY_NODE_CONNECTOR_ID = QName.create("urn:opendaylight:inventory", "2013-08-19", "id");
    private static final QName FLOW_CAPABLE_STATS = QName.create("urn:opendaylight:port:statistics", "2013-12-14", "flow-capable-node-connector-statistics");
    private static final QName BYTES_TRANSMITTED = QName.create("urn:opendaylight:port:statistics", "2013-12-14", "bytes");

    // SMART YANG Model QNames (for policy enforcement)
    private static final String SMART_NAMESPACE = "urn:opendaylight:params:xml:ns:yang:smart";
    private static final String SMART_REVISION = "2017-02-15";
    private static final QName SMART_CONTEXT = QName.create(SMART_NAMESPACE, SMART_REVISION, "smart-context");
    private static final QName FLOWS = QName.create(SMART_NAMESPACE, SMART_REVISION, "flows");
    private static final QName ID = QName.create(SMART_NAMESPACE, SMART_REVISION, "id");
    private static final QName STATUS = QName.create(SMART_NAMESPACE, SMART_REVISION, "status");
    private static final QName IS_CLONED = QName.create(SMART_NAMESPACE, SMART_REVISION, "is-cloned");

    public ResilienceManager() {
        this(null);
    }

    public ResilienceManager(DOMDataBroker domDataBroker) {
        LOG.info("Initializing SDDS Resilience Manager (SMART-backed)...");
        this.domDataBroker = domDataBroker;
    }

    /**
     * Applies a redundancy policy based on SLA thresholds and real-time congestion.
     * @param flowId The ID of the flow to protect.
     * @param policy The default policy type: CLONE, DIVERT, or REPLICATE.
     */
    public void applyPolicy(String flowId, String policy) {
        LOG.info("Evaluating SMART Policy for Flow: {}", flowId);
        
        // Proper Congestion Detection via MD-SAL DOM
        boolean isCongested = detectCongestion("node-1", "node-1:1");
        
        if (isCongested) {
            LOG.warn("CONGESTION DETECTED on flow [{}]. Triggering Selective Redundancy (SMART)...", flowId);
            executeReplication(flowId, "REPLICATE");
        } else {
            LOG.info("Flow [{}] is compliant with SLA. Standard policy [{}] applied.", flowId, policy);
        }
    }

    private boolean detectCongestion(String nodeId, String portId) {
        if (domDataBroker == null) {
            LOG.debug("DOMDataBroker not available. Falling back to stochastic simulation for research parity.");
            return random.nextDouble() < 0.3;
        }

        long linkLoad = queryLinkLoad(nodeId, portId);
        return linkLoad > 1000000L; // 1MB/s Load Threshold
    }

    /**
     * Queries the ODL Operational Data Store for real-time port statistics.
     */
    private long queryLinkLoad(String nodeId, String portId) {
        YangInstanceIdentifier path = YangInstanceIdentifier.builder()
            .node(INVENTORY_NODES)
            .nodeWithKey(INVENTORY_NODE, INVENTORY_NODE_ID, nodeId)
            .nodeWithKey(INVENTORY_NODE_CONNECTOR, INVENTORY_NODE_CONNECTOR_ID, portId)
            .node(FLOW_CAPABLE_STATS)
            .build();

        try (DOMDataReadOnlyTransaction tx = domDataBroker.newReadOnlyTransaction()) {
            Optional<NormalizedNode<?, ?>> result = tx.read(LogicalDatastoreType.OPERATIONAL, path).get();
            
            if (result.isPresent() && result.get() instanceof DataContainerNode) {
                DataContainerNode<?> statsContainer = (DataContainerNode<?>) result.get();
                // Querying for bytes leaf using NodeIdentifier
                com.google.common.base.Optional<org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode<?, ?>> bytesLeaf = 
                        (com.google.common.base.Optional) statsContainer.getChild(new YangInstanceIdentifier.NodeIdentifier(BYTES_TRANSMITTED));
                
                if (bytesLeaf.isPresent() && bytesLeaf.get() instanceof LeafNode) {
                    return (Long) ((LeafNode<?>) bytesLeaf.get()).getValue();
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to query ODL statistics for {}: {}", nodeId, e.getMessage());
        }
        return 0;
    }

    /**
     * Enforces the SMART decision by publishing an AMQP update and triggering an MD-SAL transaction.
     */
    private void executeReplication(String flowId, String action) {
        LOG.info("[SMART-ACTION] Enforcing Policy [{}] for flow {}", action, flowId);

        // 1. Construct the SMART Enhancement Tag
        String tag = "SMART_VER_1.0|FLOW_ID:" + flowId + "|DECISION:" + action;

        // 2. Publish to AMQP Topic (Coordination Plane)
        try {
            LOG.info("[SMART-ACTION] Publishing Policy Update to smart/flow-updates...");
            org.opendaylight.messaging4transport.impl.AmqpPublisher.publish("smart/flow-updates", tag);
        } catch (Exception e) {
            LOG.warn("[SMART-ACTION] AMQP Policy Publication failed: {}", e.getMessage());
        }

        // 3. MD-SAL DOM Enforcement (Local Data Store)
        if (domDataBroker != null) {
            enforceInMDSAL(flowId, action);
        }
    }

    private void enforceInMDSAL(String flowId, String action) {
        // Build the YangInstanceIdentifier for the specific flow status
        YangInstanceIdentifier path = YangInstanceIdentifier.builder()
            .node(SMART_CONTEXT)
            .node(FLOWS)
            .nodeWithKey(FLOWS, ID, flowId)
            .node(STATUS)
            .build();

        // Build the NormalizedNode (Status container with is-cloned leaf)
        org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode<?, ?> statusNode = 
            org.opendaylight.yangtools.yang.data.impl.schema.Builders.containerBuilder()
            .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(STATUS))
            .withChild(org.opendaylight.yangtools.yang.data.impl.schema.Builders.leafBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(QName.create(
                        SMART_CONTEXT.getNamespace(), SMART_CONTEXT.getRevision(), "is-cloned")))
                .withValue(action.equals("REPLICATE") || action.equals("CLONE"))
                .build())
            .build();

        try {
            org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction tx = 
                    domDataBroker.newWriteOnlyTransaction();
            tx.merge(LogicalDatastoreType.OPERATIONAL, path, statusNode);
            tx.submit().get(); // Synchronous wait for research parity
            LOG.info("[SMART-ENHANCER-DOM] Successfully merged {} to MD-SAL for {}", action, flowId);
        } catch (Exception e) {
            LOG.error("[SMART-ENHANCER-DOM] Transaction Failed for {}: {}", flowId, e.getMessage());
        }
    }
}
