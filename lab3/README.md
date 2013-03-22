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
