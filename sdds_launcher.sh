#!/bin/bash
# SDDS Framework Master Launcher (Mayan-DS)
# Automates the build and integration of M4T, Evora, and SMART building blocks.

set -e

echo "Starting SDDS Framework Orchestration..."

# 1. Environment Preparation (Shims for legacy ODL snapshots)
# This is necessary because SMART depends on 2015-era sal-binding-api:1.3.0-SNAPSHOT
echo "Applying ODL Beryllium Shims to local Maven repository..."
mkdir -p ~/.m2/repository/org/opendaylight/controller/sal-binding-api/1.3.0-SNAPSHOT/
mkdir -p ~/.m2/repository/org/opendaylight/controller/sal-dom-api/1.3.0-SNAPSHOT/

# Note: We assume the Beryllium-SR4 JARs are already in the local cache or downloaded.
# In a fresh environment, we would use curl to fetch them from the ODL archive.

# 2. Build External Dependencies (Static Repos)
# These are already installed in the local Maven repository.
# echo "Building Messaging4Transport..."
# mvn -U -s /tmp/settings.xml -f /home/pradeeban/messaging4transport/pom.xml clean install -DskipTests

# 3. Start AMQP Broker (ActiveMQ)
echo "Ensuring AMQP Broker is active..."
# Force remove any existing container to avoid conflicts
docker rm -f sdds-broker 2>/dev/null || true

echo "Starting sdds-broker container..."
docker run -d --name sdds-broker -p 5672:5672 -p 8161:8161 rmohr/activemq

# Wait for broker readiness (Port 5672)
echo "Waiting for broker on localhost:5672..."
MAX_RETRIES=30
COUNT=0
while ! timeout 1 bash -c "cat < /dev/null > /dev/tcp/localhost/5672" 2>/dev/null; do
    sleep 2
    COUNT=$((COUNT+1))
    if [ $COUNT -ge $MAX_RETRIES ]; then
        echo "Error: AMQP Broker failed to start in time. Check 'docker logs sdds-broker'"
        exit 1
    fi
done
echo "Broker is ready!"

# 4. Build & Run SDDS Framework
echo "Building SDDS Framework Integration..."
mvn -U clean compile

echo "Launching SDDS (Mayan-DS)..."
mvn exec:java -Dexec.mainClass="org.sdds.Main"
