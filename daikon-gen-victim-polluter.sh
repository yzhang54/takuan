#!/usr/bin/env bash

trap "exit" INT

if [[ -z "${NO_DYNCOMP}" ]]; then
    COMPARABILITY_FILE='--comparability-file=Runner.decls-DynComp'
else
    COMPARABILITY_FILE=''
fi

if [[ -n "${IGNORED_CLASSES}" ]]; then
    BOOT_CLASSES="--boot-classes=$IGNORED_CLASSES"
else 
    BOOT_CLASSES=''
fi

if [[ -n "${PPT_SELECT}" ]]; then
    PPT_SELECT_PATTERN="--ppt-select-pattern=$PPT_SELECT"
else
    PPT_SELECT_PATTERN=''
fi

run_daikon() {
    local TYPE=$1
    local SHOULD=$2
    shift; shift

    if [ "$SHOULD" = "pass" ]; then
        if [[ -z "${NO_DYNCOMP}" ]]; then
            # TODO: we should retry if it fails, but only if it fails for an actual reason, not because of the various DynComp bugs
            java -cp "./target/dependency/*:./target/classes:./target/test-classes:$DAIKONDIR/daikon.jar:$(dirname "$0")/runner-1.0-SNAPSHOT.jar:$CLASSPATH" daikon.DynComp "$PPT_SELECT_PATTERN" --ppt-omit-pattern='org.junit|junit.framework|junit.runner|com.sun.proxy|javax.servlet|org.hamcrest|in.natelev.runner|groovyjarjarasm.asm' in.natelev.runner.Runner $@
        fi

        while ! java -cp "./target/dependency/*:./target/classes:./target/test-classes:$DAIKONDIR/daikon.jar:$(dirname "$0")/runner-1.0-SNAPSHOT.jar:$CLASSPATH" daikon.Chicory "$PPT_SELECT_PATTERN" --ppt-omit-pattern='org.junit|junit.framework|junit.runner|com.sun.proxy|javax.servlet|org.hamcrest|in.natelev.runner|groovyjarjarasm.asm' $BOOT_CLASSES $COMPARABILITY_FILE in.natelev.runner.Runner $@
        do echo "Re-trying daikon.Chicory because the test(s) failed..."; sleep 1; done;
    else 
        if [[ -z "${NO_DYNCOMP}" ]]; then
            while java -cp "./target/dependency/*:./target/classes:./target/test-classes:$DAIKONDIR/daikon.jar:$(dirname "$0")/runner-1.0-SNAPSHOT.jar:$CLASSPATH" daikon.DynComp "$PPT_SELECT_PATTERN" --ppt-omit-pattern='org.junit|junit.framework|junit.runner|com.sun.proxy|javax.servlet|org.hamcrest|in.natelev.runner|groovyjarjarasm.asm' in.natelev.runner.Runner $@
            do echo "Re-trying daikon.DynComp because the test(s) passed..."; sleep 1; done;
        fi

        while java -cp "./target/dependency/*:./target/classes:./target/test-classes:$DAIKONDIR/daikon.jar:$(dirname "$0")/runner-1.0-SNAPSHOT.jar:$CLASSPATH" daikon.Chicory "$PPT_SELECT_PATTERN" --ppt-omit-pattern='org.junit|junit.framework|junit.runner|com.sun.proxy|javax.servlet|org.hamcrest|in.natelev.runner|groovyjarjarasm.asm' $BOOT_CLASSES $COMPARABILITY_FILE in.natelev.runner.Runner $@
        do echo "Re-trying daikon.Chicory because the test(s) passed..."; sleep 1; done;
    fi

    echo "Running Daikon..."

    java -XX:MaxRAMPercentage=50.0 -cp $DAIKONDIR/daikon.jar daikon.Daikon Runner.dtrace.gz >"daikon-$TYPE.log"

    mv Runner.inv.gz "daikon-$TYPE.inv.gz"

    gunzip "daikon-$TYPE.inv.gz"

    rm Runner.dtrace.gz
    [[ -z "${NO_DYNCOMP}" ]] && rm Runner.decls-DynComp

    echo "Completed '$TYPE'. Daikon text output is in daikon-$TYPE.log, binary output is in daikon-$TYPE.inv"
}

if [ "$1" = "" ] || [ "$2" = "" ]; then
    echo "Usage: ./daikon-victim-polluter.sh <com.example.VictimTest> <com.example.PolluterTest>"
elif [ "$1" = "test" ]; then
    shift
    VICTIM=$1
    POLLUTER=$2
    
    java -cp "./target/dependency/*:./target/classes:./target/test-classes:$DAIKONDIR/daikon.jar:$(dirname "$0")/runner-1.0-SNAPSHOT.jar" in.natelev.runner.Runner $POLLUTER $VICTIM
else
    VICTIM=$1
    POLLUTER=$2

    # if you are in a multi-module project, you may have to go to the project root and run `mvn install`
    if [[ -z "${NO_RECOMPILATION}" ]]; then
        mvn dependency:copy-dependencies
        mvn package -Dmaven.test.skip=true
        mvn compile
        mvn test-compile
    fi

    run_daikon "pv" "fail" "$POLLUTER" "$VICTIM"
    run_daikon "polluter" "pass" "$POLLUTER"
    run_daikon "victim" "pass" "$VICTIM"
fi;


