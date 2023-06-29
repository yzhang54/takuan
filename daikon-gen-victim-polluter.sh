#!/usr/bin/env bash

run_daikon() {
    local TYPE=$1
    local SHOULD=$2
    shift; shift

    if [ "$SHOULD" = "pass" ]; then
        # TODO: we should retry if it fails, but only if it fails for an actual reason, not because of the `extends` bug in DynComp
        java -cp "./target/dependency/*:./target/classes:./target/test-classes:$DAIKONDIR/daikon.jar" daikon.DynComp --ppt-omit-pattern='org.junit|junit.framework|junit.runner|com.sun.proxy|javax.servlet|org.hamcrest' org.junit.runner.JUnitCore $@

        while ! java -cp "./target/dependency/*:./target/classes:./target/test-classes:$DAIKONDIR/daikon.jar" daikon.Chicory --ppt-omit-pattern='org.junit|junit.framework|junit.runner|com.sun.proxy|javax.servlet|org.hamcrest' --comparability-file=JUnitCore.decls-DynComp org.junit.runner.JUnitCore $@
        do echo "Re-trying daikon.Chicory because the test(s) failed..."; sleep 1; done;
    else 
        while java -cp "./target/dependency/*:./target/classes:./target/test-classes:$DAIKONDIR/daikon.jar" daikon.DynComp --ppt-omit-pattern='org.junit|junit.framework|junit.runner|com.sun.proxy|javax.servlet|org.hamcrest' org.junit.runner.JUnitCore $@
        do echo "Re-trying daikon.DynComp because the test(s) passed..."; sleep 1; done;

        while java -cp "./target/dependency/*:./target/classes:./target/test-classes:$DAIKONDIR/daikon.jar" daikon.Chicory --ppt-omit-pattern='org.junit|junit.framework|junit.runner|com.sun.proxy|javax.servlet|org.hamcrest' --comparability-file=JUnitCore.decls-DynComp org.junit.runner.JUnitCore $@
        do echo "Re-trying daikon.Chicory because the test(s) passed..."; sleep 1; done;
    fi

    echo "Running Daikon..."

    java -XX:MaxRAMPercentage=50.0 -cp $DAIKONDIR/daikon.jar daikon.Daikon JUnitCore.dtrace.gz >"daikon-$TYPE.log"

    mv JUnitCore.inv.gz "daikon-$TYPE.inv.gz"

    gunzip "daikon-$TYPE.inv.gz"

    echo "Completed '$TYPE'. Daikon text output is in daikon-$TYPE.log, binary output is in daikon-$TYPE.inv"
}

if [ "$1" = "" ] || [ "$2" = "" ]; then
    echo "Usage: ./daikon-victim-polluter.sh <com.example.VictimTest> <com.example.PolluterTest>"
elif [ "$1" = "test" ]; then
    shift
    VICTIM=$1
    POLLUTER=$2
    
    java -cp "./target/dependency/*:./target/classes:./target/test-classes:$DAIKONDIR/daikon.jar" org.junit.runner.JUnitCore $POLLUTER $VICTIM
else
    VICTIM=$1
    POLLUTER=$2

    # if you are in a multi-module project, you may have to go to the project root and run `mvn install`
    mvn dependency:copy-dependencies
    mvn package -Dmaven.test.skip=true

    # TODO: support Class#method syntax and auto-comment out all the other tests of the classes

    run_daikon "pv" "fail" "$POLLUTER" "$VICTIM"
    run_daikon "polluter" "pass" "$POLLUTER"
    run_daikon "victim" "pass" "$VICTIM"
fi;


