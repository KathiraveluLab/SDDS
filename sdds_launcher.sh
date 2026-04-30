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

PORT=5672
# Check if port 5672 is in use
if nc -z localhost 5672 2>/dev/null; then
    echo "Port 5672 is already in use."
    
    IS_ACTIVEMQ=false
    # 1. Check if it's an ActiveMQ Docker container
    if docker ps --filter "publish=5672" --format "{{.Image}} {{.Names}}" | grep -iq "activemq"; then
        echo "Port 5672 is used by an ActiveMQ Docker container."
        IS_ACTIVEMQ=true
    # 2. Check if it's an ActiveMQ process on the host
    elif ps -ef | grep -i "activemq.jar" | grep -v "grep" > /dev/null; then
        echo "Port 5672 is occupied, and an ActiveMQ process was found on the host."
        IS_ACTIVEMQ=true
    fi

    if [ "$IS_ACTIVEMQ" = "true" ]; then
        echo "Simply using the existing ActiveMQ instance on port 5672 as requested."
    else
        echo "Port 5672 is occupied by another program. Spawning ActiveMQ on port 5673."
        PORT=5673
        docker rm -f sdds-broker 2>/dev/null || true
        docker run -d --name sdds-broker -p $PORT:5672 -p 8161:8161 rmohr/activemq
    fi
else
    echo "Port 5672 is free. Starting sdds-broker container..."
    docker rm -f sdds-broker 2>/dev/null || true
    docker run -d --name sdds-broker -p 5672:5672 -p 8161:8161 rmohr/activemq
fi

# Wait for broker readiness
echo "Waiting for broker on localhost:$PORT..."
MAX_RETRIES=30
COUNT=0
while ! timeout 1 bash -c "cat < /dev/null > /dev/tcp/localhost/$PORT" 2>/dev/null; do
    sleep 2
    COUNT=$((COUNT+1))
    if [ $COUNT -ge $MAX_RETRIES ]; then
        echo "Error: AMQP Broker failed to start in time. Check 'docker logs sdds-broker' or existing broker."
        exit 1
    fi
done
echo "Broker is ready on port $PORT!"

# 4. Build & Run SDDS Framework
echo "Building SDDS Framework Integration..."
mvn -U clean compile

echo "Launching SDDS (Mayan-DS)..."
echo "Refer to USER-GUIDE.md for instructions on using the sdds_client.sh and monitoring the system."
mvn exec:java -Dexec.mainClass="org.sdds.Main" -Damqp.port=$PORT
