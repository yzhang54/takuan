#!/bin/bash

# this script should be run with cwd=project root.

if [ "$#" != 3 ]; then
    echo "Usage: ./findPatch.sh <polluter> <victim> <cleanerJsonFilePath>"
    exit 1;
fi

minimizedPath="./.dtfixingtools/minimized/"
detectionResultsPath="./.dtfixingtools/detection-results/"
orginalOrderFilePath="./.dtfixingtools/original-order.json"
polluter="$1"
victim="$2"
cleanerJsonFilePath="$3"
resultFolderPath="./.dtfixingtools/test-runs/results"

echo "Confirming that the suspected cleaner is a cleaner (that victim passes)..."

if [ ! -d "./.dtfixingtools" ]; then
  mkdir -p "./.dtfixingtools";
fi

export cleanerName=$(jq -r '.cleaners[].testMethod' $cleanerJsonFilePath)
cat > $orginalOrderFilePath << EOL
$polluter
$cleanerName
$victim
EOL

if ! mvn idflakies:detect -Ddetector.detector_type=original -Ddt.randomize.rounds=0 -Ddt.detector.original_order.all_must_pass=true -Ddt.original.order=$orginalOrderFilePath
then 
	echo "Err: Failed to confirm that suspected cleaner is a cleaner"
	exit 0
fi 

tmp=$(shopt -p nullglob || true)
shopt -s nullglob
declare -a resultFile=($resultFolderPath/*)
if (( ${#resultFile[@]} != 1 )); then
  printf "error: zero or more than one file in $resultFile"
  exit 1
else
    if grep -Fxq "ERROR" "${resultFile[0]}"
    then
          printf 'ERROR FOUND, Tests failed'
          exit 1
    fi
fi
eval "$tmp"

cwd=$(pwd)
echo "Finding patch in: $cwd"
if [ ! -d $minimizedPath ]; then
  mkdir -p $minimizedPath;
fi
if [ ! -d $detectionResultsPath ]; then
  mkdir -p $detectionResultsPath;
fi

SCRIPTS_DIR=$(dirname "$0")

java -cp "$SCRIPTS_DIR/../target/classes:$SCRIPTS_DIR/../target/dependency/*" in.yulez.patch.Patch $@ $cwd
# execute idflakies on the mvn project to get fixier.
#cd $mvnProjectLocalPath
echo "Finding patch in: $cwd"
mvn clean install compile -Dmaven.test.skip=true

mvn idflakies:fix
