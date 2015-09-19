#!/bin/sh

# The following script starts the service directly from the source
vertx run App.java -cp src/main/java:src/main/resources &

# Alternatively, if you want to run it from a maven build artifact
# This project has been setup to build a fat jar
# Running it is as simple as
# java -jar target/vtx-demo-1.0.0-fat.jar

