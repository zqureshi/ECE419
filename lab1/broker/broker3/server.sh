#!/bin/bash
# server.sh
ECE419_HOME=/cad2/ece419s/
JAVA_HOME=${ECE419_HOME}/java/jdk1.6.0/

# arguments to OnlineBroker
# $1 = hostname of BrokerLookupServer
# $2 = port where BrokerLookupServer is listening
# $3 = port where I will be listening
# $4 = my name ("nasdaq" or "tse")

${JAVA_HOME}/bin/java OnlineBroker $1 $2 $3 $4






