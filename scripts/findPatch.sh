#!/bin/bash

# this script should be run with cwd=project root.
if [ "$#" != 4 ]; then
    echo "Usage: ./findPatch.sh <polluter> <victim> <cleanerJsonFilePath> <mvnProjectLocalPath>"
    exit 1;
fi



mvnProjectLocalPath="$4"
minimizedPath="$mvnProjectLocalPath/.dtfixingtools/minimized/"
detectionResultsPath="$mvnProjectLocalPath/.dtfixingtools/detection-results/"
orginalOrderFilePath="$mvnProjectLocalPath/.dtfixingtools/original-order.json"
polluter="$1"
victim="$2"
cleanerJsonFilePath="$3"
resultFolderPath="$mvnProjectLocalPath/.dtfixingtools/test-runs/results"

echo "Confirm that a suspected cleaner is a cleaner to ensure victim passes"
mkdir -p $mvnProjectLocalPath/.dtfixingtools/
export cleanerName=$(jq -r '.cleaners[].testMethod' $cleanerJsonFilePath)
cat > $orginalOrderFilePath << EOL
$polluter
$cleanerName
$victim
EOL



cd $mvnProjectLocalPath
mvn idflakies:detect -Ddetector.detector_type=original -Ddt.randomize.rounds=0 -Ddt.detector.original_order.all_must_pass=true -Ddt.original.order=$orginalOrderFilePath


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
java -cp "$SCRIPTS_DIR/../target/classes:$SCRIPTS_DIR/../target/dependency/*" in.yulez.patch.Patch $@
# execute idflakies on the mvn project to get fixier.
cd $mvnProjectLocalPath
mvn idflakies:fix
