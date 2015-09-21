#!/bin/sh

VERTX_OPTS="-Dvertx.deployment.options.redeploy=true -Dvertx.disableFileCaching=true"

# The following script starts the io.fuzz.service directly from the source
vertx run App.java -Dvertx.deployment.options.redeploy=true -cp src/main/java:src/main/resources

# Alternatively, if you want to run it from a maven build artifact
# This project has been setup to build a fat jar
# Running it is as simple as
# java -jar target/vtx-demo-1.0.0-fat.jar

