#!/bin/sh

MAVEN=target/clickstream-analysis-1.0-SNAPSHOT.jar
GRADLE=build/libs/clickstream-analysis-1.0-SNAPSHOT.jar
ls -l --block-size=M $MAVEN | awk '{print $9 $5}'
ls -l --block-size=M $GRADLE | awk '{print $9 $5}'