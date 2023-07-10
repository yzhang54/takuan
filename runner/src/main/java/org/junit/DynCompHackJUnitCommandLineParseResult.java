package org.junit;

// this class tricks DynComp into thinking that JUnit is running, as DynComp doesn't
// pick up on the way we call it (DynComp assumes that JUnit is parsing the CLI args)
//
// it adds a class that contains "JUnitCommandLineParseResult" w/the method `parse`
// to the call stack, which is what DynComp looks for.
//
// this fixes the "Test class can only have one constructor" error and related problems
public class DynCompHackJUnitCommandLineParseResult {
    public static void parse(Runnable runnable) {
        runnable.run();
    }
}
