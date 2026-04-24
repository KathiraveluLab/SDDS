#!/bin/bash
# SDDS Dynamic Command Client
# Usage: ./sdds_client.sh <COMMAND> <PAYLOAD>

if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <COMMAND> <PAYLOAD>"
    echo "Example: ./sdds_client.sh SCHEDULE compression"
    echo "Example: ./sdds_client.sh POLICY flow-102,CLONE"
    exit 1
fi

mvn exec:java -Dexec.mainClass="org.sdds.ControlClient" -Dexec.args="$1 $2" -q
