#!/bin/bash
# client.sh
ECE419_HOME=/cad2/ece419s/
JAVA_HOME=${ECE419_HOME}/java/jdk1.6.0/

# arguments to BrokerExchange
# $1 = hostname of where BrokerLookupServer is located
# $2 = port # where BrokerLookupServer is listening
# $3 = name of broker you are connecting to ("nasdaq" or "tse")

${JAVA_HOME}/bin/java BrokerExchange $1 $2 $3




