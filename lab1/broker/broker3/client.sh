#!/bin/bash
# client.sh
ECE419_HOME=/cad2/ece419s/
JAVA_HOME=${ECE419_HOME}/java/jdk1.6.0/

# arguments to BrokerClient
# $1 = hostname of where BrokerLookupServer is located
# $2 = port # where BrokerLookupServer is listening

${JAVA_HOME}/bin/java BrokerClient $1 $2



