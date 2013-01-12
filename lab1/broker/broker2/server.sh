#!/bin/bash
# server.sh
ECE419_HOME=/cad2/ece419s/
JAVA_HOME=${ECE419_HOME}/java/jdk1.6.0/

# arguments to OnlineBroker
# $1 = listening port

${JAVA_HOME}/bin/java OnlineBroker $1




