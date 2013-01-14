#!/bin/bash
# client.sh

# arguments to BrokerExchange
# $1 = hostname of where BrokerLookupServer is located
# $2 = port # where BrokerLookupServer is listening
# $3 = name of broker you are connecting to ("nasdaq" or "tse")

java BrokerExchange $1 $2 $3




