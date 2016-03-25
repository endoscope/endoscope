#!/bin/bash
mvn release:clean release:prepare

# manually
# mvn versions:set -DnewVersion=1.2.3
# mvn clean deploy -P release