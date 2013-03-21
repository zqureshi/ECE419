% ECE419 Lab 2
% Zeeshan Qureshi, Jaideep Bajwa
% 21 January 2012

Requirements
============

  + Ant (build sytem)
  + Ivy (dependency management)
  + Guava (event bus)

Usage
=====

Install dependencies and build project:

    $ ant

Run Server:

    $ ./server.sh 8000

Run Clients:

    $ ./client.sh localhost 8000

Design Decisions
================
For this lab we decided that we did not want to complicate the design a
lot and thus only support adding clients at the start of a game. Once a
player makes a move the game starts and no other clients can join. We
use the deterministic property of the pseudo-random number generator to
guarantee that all clients put new players on the exact same spot and
orientation in their display.
