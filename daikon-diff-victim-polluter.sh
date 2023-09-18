#!/usr/bin/env bash

java -cp "$(dirname "$0")/target/classes:$DAIKONDIR/daikon.jar" -Xmx6g -XX:+UseG1GC in.natelev.daikondiffvictimpolluter.DaikonDiffVictimPolluter daikon-pv.inv daikon-victim.inv daikon-polluter.inv $@