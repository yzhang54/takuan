package in.natelev.runner;

import java.util.Arrays;

import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class Runner {
    public static String RESET = "";
    public static String RED = "";
    public static String GREEN = "";
    public static String YELLOW = "";

    // to allow for redirection to a file
    static {
        if (System.console() != null) {
            RESET = "\u001B[0m";
            RED = "\u001B[31m";
            GREEN = "\u001B[32m";
            YELLOW = "\u001B[33m";
        }
    }

    public static void main(String[] args) throws ClassNotFoundException {
        JUnitCore junit = new JUnitCore();
        int exitCode = 0;
        for (String arg : args) {
            // each arg must be in the form com.example.ClassName.methodName
            String[] parts = arg.split("\\.");
            if (parts.length < 2) {
                System.out.println("Invalid argument: " + arg);
                System.exit(1);
            }
            String method = parts[parts.length - 1];
            String clazzName = String.join(".", Arrays.copyOfRange(parts, 0, parts.length - 1));
            System.out.println(YELLOW + "Running " + clazzName + "#" + method + RESET);
            Class<?> clazz = Class.forName(clazzName);
            Result result = junit.run(Request.method(clazz, method));
            if (result.wasSuccessful()) {
                System.out.println(GREEN + "✓ " + arg + " passed" + RESET);
            } else {
                System.out.println(RED + "\u2717 " + arg + " failed" + RESET);
                exitCode = 1;
                for (Failure failure : result.getFailures()) {
                    System.out.println(failure.getTestHeader());
                    System.out.println(failure.getMessage() + " " + failure.getDescription());
                    failure.getException().printStackTrace();
                }
            }
        }
        System.exit(exitCode);
    }
}
