% ECE419 Lab 3
% Zeeshan Qureshi; Jaideep Bajwa
% 22 Mar 2013

Requirements
============

  + Ant (build sytem)
  + Ivy (dependency management)
  + Guava (event bus)
  + ZeroMQ (Distributed Message Queue)
  + ZooKeeper (Distributed Coordination Service)

Usage
=====

Install dependencies and build project:

    $ ant

Run Clients:

    $ ./client.sh localhost 8000 {client-port} {game-name} [player-name]

Design Decisions
================
For this lab we decided that we did not want to complicate the design a
lot and thus only support adding clients at the start of a game. Once a
player makes a move the game starts and no other clients can join. We
use the deterministic property of the pseudo-random number generator to
guarantee that all clients put new players on the exact same spot and
orientation in their display.

Naming/Coordination Service
===========================

We used ZooKeeper1 as our centralized naming service. It will provide the following:


a. Heartbeat service.
b. Store current client list.
c. Failure detection. For example, if it doesn’t receive 5 consecutive heartbeats from a client, that client is considered dead and is removed from the list.
d. A game server ID which will allow us to host multiple gaming sessions.
New clients get to join games of their preference.


Message Queue
============

We used ZeroMQ2 as our message queue.  It provides the following:


a. Connection setup and tear-down. b. One-many messaging (fan-out).
c. Many-one messaging (fan-in).


Sequencing for ordering
======================

We used "zookeeper" as our sequencer which allowed us to provide a global ordering of events.

Q/A
===

Ques: Evaluate the portion of your design that deals with starting, maintaining, and exiting a game, what are its strengths and weaknesses?

Ans: We have used zookeeper as our coordination system.
+ Everytime a client starts, it connects with the zookeeper.
+ zookeeper creates a znode, under the root directory "/game-<id>" (createmode =  sequential,ephemeral)
+ the client sets a watch on its parent and its siblings if any
The above mechanism is very robust and gaurantees the following
+ Whenever a player joins, all players are notified (nodechildren changed in the parent)
+ Whenever a player quits/ or looses connection, all other siblings will be notified and they update their respective maze

Weakness :
+ Do not support dynamic join.

Ques: Evaluate your design with respect to its performance on the current platform (i.e. ug machines in a small LAN). If applicable, you can use the robot clients in Mazewar to measure the number of packets sent for various time intervals and number of players. Analyze your results.
Ans: need to do some packet sniffing.

Ques: How does your current design scale for an increased number of players? What if it is played across a higher-latency, lower-bandwidth wireless network { high packet loss rates? What if played on a mix of mobile devices, laptops, computers, wired/wireless?
Ans: As we rely on packet sequence number for ordering, if a client has higer-latency it will affect everyone in the game. Each client will display an action associated with a packet recieved only if its sequence number matches with the sequence number provided by our sequencer (zookeeper) 

Ques: Evaluate your design for consistency. What inconsistencies can occur? How are they dealt with?
Ans:
+ Inconsistenciy : ordering of events can be incorrect if a clients has high latency
  Mitigation : sequencing mechanism guarantees the ordering of events.

We havent dealt with making "fire" action as atomic, so there is some inconsistency. For eg: X kills another client Y and the Y is destroyed on X's computer, but due to network delay on Y's computer it dodged the bullet and survived. 
