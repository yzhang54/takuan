#!/usr/bin/env bash

# to add dependencies to classpath: mvn dependency:copy-dependencies

run_daikon() {
    local TYPE=$1
    local SHOULD=$2
    shift; shift

    if [ "$SHOULD" = "pass" ]; then
        while ! java -cp "./target/dependency/*:./target/classes:./target/test-classes:$DAIKONDIR/daikon.jar" daikon.DynComp --ppt-omit-pattern='org.junit|junit.framework|junit.runner|com.sun.proxy.' org.junit.runner.JUnitCore $@
        do echo "Re-trying daikon.DynComp because the test(s) failed..."; sleep 1; done;

        while ! java -cp "./target/dependency/*:./target/classes:./target/test-classes:$DAIKONDIR/daikon.jar" daikon.Chicory --ppt-omit-pattern='org.junit|junit.framework|junit.runner|com.sun.proxy.' --comparability-file=JUnitCore.decls-DynComp org.junit.runner.JUnitCore $@
        do echo "Re-trying daikon.Chicory because the test(s) failed..."; sleep 1; done;
    else 
        while java -cp "./target/dependency/*:./target/classes:./target/test-classes:$DAIKONDIR/daikon.jar" daikon.DynComp --ppt-omit-pattern='org.junit|junit.framework|junit.runner|com.sun.proxy.' org.junit.runner.JUnitCore $@
        do echo "Re-trying daikon.DynComp because the test(s) passed..."; sleep 1; done;

        while java -cp "./target/dependency/*:./target/classes:./target/test-classes:$DAIKONDIR/daikon.jar" daikon.Chicory --ppt-omit-pattern='org.junit|junit.framework|junit.runner|com.sun.proxy.' --comparability-file=JUnitCore.decls-DynComp org.junit.runner.JUnitCore $@
        do echo "Re-trying daikon.Chicory because the test(s) passed..."; sleep 1; done;
    fi

    java -cp $DAIKONDIR/daikon.jar daikon.Daikon JUnitCore.dtrace.gz >"daikon-$TYPE.log"

    mv JUnitCore.inv.gz "daikon-$TYPE.inv.gz"

    gunzip "daikon-$TYPE.inv.gz"

    echo "Completed '$TYPE'. Daikon text output is in daikon-$TYPE.log, binary output is in daikon-$TYPE.inv"
}

if [ "$1" = "" ] || [ "$2" = "" ]; then
    echo "Usage: ./daikon-victim-polluter.sh <com.example.VictimTest> <com.example.PolluterTest>"
else
    VICTIM=$1
    POLLUTER=$2

    mvn dependency:copy-dependencies
    mvn package

    # TODO: support Class#method syntax and auto-comment out all the other tests of the classes

    run_daikon "pv" "fail" "$POLLUTER" "$VICTIM"
    run_daikon "polluter" "pass" "$POLLUTER"
    run_daikon "victim" "pass" "$VICTIM"
fi;


