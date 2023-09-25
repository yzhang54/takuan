#!/bin/bash

# this script should be run with cwd=project root.

if [ "$1" = "" ] || [ "$2" = "" ] || [ "$3" = "" ] ｜｜ [ "$4" = "" ]; then
    echo "Usage: ./findPatch.sh <polluter> <victim> <cleanerJsonFilePath> <mvnProjectLocalPath>"
    exit 1;
fi


mvnProjectLocalPath="$4"
minimizedPath="$mvnProjectLocalPath/.dtfixingtools/minimized/"
detectionResultsPath="$mvnProjectLocalPath/.dtfixingtools/detection-results/"

cwd=$(pwd)
echo "Finding patch in: $cwd"

if [ ! -d $minimizedPath ]; then
  mkdir -p $minimizedPath;
fi
if [ ! -d $detectionResultsPath ]; then
  mkdir -p $detectionResultsPath;
fi


SCRIPTS_DIR=$(dirname "$0")
java -cp "$SCRIPTS_DIR/../target/classes:$SCRIPTS_DIR/../target/dependency/*" in.yulez.patch.Patch $@
echo "Finding patch in: $mvnProjectLocalPath"
# execute idflakies on the mvn project to get fixier.
cd $mvnProjectLocalPath
mvn idflakies:fix
