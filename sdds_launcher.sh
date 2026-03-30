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

# echo "Building Évora..."
# mvn -U -s /tmp/settings.xml -f /home/pradeeban/KathiraveluLab/Evora/pom.xml clean install -DskipTests

# echo "Building SMART (API)..."
# mvn -U -s /tmp/settings.xml -f /home/pradeeban/SMART/pom.xml clean install -DskipTests -pl api

# 3. Build & Run SDDS Framework
echo "Building SDDS Framework Integration..."
mvn -U clean compile

echo "Launching SDDS (Mayan-DS)..."
mvn exec:java -Dexec.mainClass="org.sdds.Main"
