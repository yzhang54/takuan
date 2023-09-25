#!/bin/bash

# this script should be run with cwd=project root.
cwd=$(pwd)
echo "Finding patch in: $cwd"

if [ ! -d .dtfixingtools/minimized/ ]; then
  mkdir -p .dtfixingtools/minimized/;
fi
if [ ! -d .dtfixingtools/detection-results/ ]; then
  mkdir -p .dtfixingtools/detection-results/;
fi

# run patch program.
SCRIPTS_DIR=$(dirname "$0")
java -cp "$SCRIPTS_DIR/../target/classes:$SCRIPTS_DIR/../target/dependency/*" in.yulez.patch.Patch $@

# execute idflakies on the mvn project to get fixier.
mvn idflakies:fix