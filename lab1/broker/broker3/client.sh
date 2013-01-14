#!/bin/bash
# client.sh

# arguments to BrokerClient
# $1 = hostname of where BrokerLookupServer is located
# $2 = port # where BrokerLookupServer is listening

java BrokerClient $1 $2



