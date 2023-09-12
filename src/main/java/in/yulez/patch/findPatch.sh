#!/bin/bash



export JAVA_PROGRAM_ARGS=`echo "$@"`


cwd=$(pwd)
#cd "${cwd}"
echo "${cwd}"



if [ ! -d .dtfixingtools/minimized/ ]; then
  mkdir -p .dtfixingtools/minimized/;
fi
if [ ! -d .dtfixingtools/detection-results/ ]; then
  mkdir -p .dtfixingtools/detection-results/;
fi


# run patch program.
mvn exec:java -Dexec.mainClass=Patch.Main -Dexec.args="$JAVA_PROGRAM_ARGS"


# execute idflakies on the mvn project to get fixier.
mvn idflakies:fix