#!/bin/bash
# server.sh

# arguments to OnlineBroker
# $1 = hostname of BrokerLookupServer
# $2 = port where BrokerLookupServer is listening
# $3 = port where I will be listening
# $4 = my name ("nasdaq" or "tse")

java -cp build/ OnlineBroker $1 $2 $3 $4






