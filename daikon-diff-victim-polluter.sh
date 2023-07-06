#!/usr/bin/env bash

java -cp "$(dirname "$0")/target/classes:$(dirname "$0")/daikon-5.8.16.jar" in.natelev.daikondiffvictimpolluter.DaikonDiffVictimPolluter daikon-pv.inv daikon-victim.inv daikon-polluter.inv $@