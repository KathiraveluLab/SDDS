# Software-Defined Data Services (SDDS): Mayan-DS

The **Software-Defined Data Services (SDDS)** framework, specifically the **Mayan-DS** implementation, is a network-aware middleware designed for big data executions at internet scale. It decouples the management logic from the data service plane, enabling dynamic orchestration and resilient execution through three functional planes.

## Architecture: Mayan-DS

Mayan-DS orchestrates data services by integrating core building blocks from the KathiraveluLab research portfolio:

1.  **Control Plane (Messaging4Transport)**: 
    - Handles asynchronous control flows using AMQP 1.0 and MD-SAL.
    - Enables cross-domain coordination between data service nodes.
2.  **Orchestration Plane (Évora)**: 
    - Implements network-aware service placement using greedy optimization.
    - Minimizes cost and latency penalties along the data path.
3.  **Resilience Plane (SMART)**: 
    - Provides differentiated QoS and selective redundancy.
    - Monitors link congestion via MD-SAL DOM telemetry and triggers policy-based replication.

## Getting Started

### Prerequisites
- **Java 8** (JDK 1.8 compatibility)
- **Maven 3.8+**
- (Optional) **ActiveMQ** broker listening on `localhost:5672` for AMQP communication.

### Build and Launch
The framework is pre-configured with the necessary OpenDaylight Beryllium-SR4 shims. Use the master launcher for a one-click deployment:

```bash
chmod +x sdds_launcher.sh
./sdds_launcher.sh
```

## 📂 Project Structure
- `src/main/java/org/sdds/Main.java`: System entry point.
- `src/main/java/org/sdds/ControlChannel.java`: Control plane implementation (M4T).
- `src/main/java/org/sdds/Orchestrator.java`: Orchestration plane implementation (Évora).
- `src/main/java/org/sdds/ResilienceManager.java`: Resilience plane implementation (SMART).


## Citing SDDS

If you use Mayan-DS, the Software-Defined Data Services (SDDS) framework in your research, please cite the following papers:

* Kathiravelu, P., Van Roy, P. and Veiga, L., 2020. **Interoperable and network‐aware service workflows for big data executions at internet scale.** Concurrency and Computation: Practice and Experience, 32(21), p.e5212.
  
* Kathiravelu, P., Van Roy, P. and Veiga, L., 2018, April. **Software-defined data services: Interoperable and network-aware big data executions.** In 2018 Fifth International Conference on Software Defined Systems (SDS) (pp. 145-152). IEEE.