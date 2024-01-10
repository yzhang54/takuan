package in.natelev.runner;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.manipulation.Filter;
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

    public static PolluterRerunner polluterRerunner;

    public static void main(String[] args) {
        JUnitCore junit = new JUnitCore();
        int exitCode = 0;

        try {
            if (args.length == 0) {
                System.err.println(RED + "No tests specified" + RESET);
                System.exit(1);
            } else if (args.length == 1) {
                TestArg test = new TestArg(args[0]);
                System.out.println(YELLOW + "Running " + test + RESET);
                Result result = junit.run(Request.method(test.getTestClass(), test.getMethod()));
                exitCode = printResult(result, test.toString());
            } else if (args.length == 2) {
                if (args[0].equals("all")) {
                    TestArg polluterTest = new TestArg(args[1]);

                    System.out.println(YELLOW + "Running polluter first" + RESET);
                    Request polluterRequest = Request.aClass(polluterTest.getTestClass()).filterWith(new Filter() {
                        @Override
                        public boolean shouldRun(Description description) {
                            return polluterTest.getMethod().equals(description.getMethodName());
                        }

                        @Override
                        public String describe() {
                            return "Only run polluter";
                        }
                    });
                    junit.run(polluterRequest);

                    polluterRerunner = new PolluterRerunner(polluterRequest, junit,
                            polluterTest.getMethod());

                    Request request = Request.classes(getAllTestClasses()).filterWith(new Filter() {
                        @Override
                        public boolean shouldRun(Description description) {
                            return !polluterTest.getMethod().equals(description.getMethodName());
                        }

                        @Override
                        public String describe() {
                            return "Don't rerun polluter";
                        }
                    });

                    System.out.println(YELLOW + "Running all tests" + RESET);
                    junit.run(request);
                    return;
                }

                TestArg test1 = new TestArg(args[0]);
                TestArg test2 = new TestArg(args[1]);
                if (test1.getClassName().equals(test2.getClassName())) {
                    // run in same Runner
                    System.out.println(YELLOW + "Running " + test1 + " and " + test2 + RESET);
                    Request request = Request.aClass(test1.getTestClass()).filterWith(new Filter() {
                        @Override
                        public boolean shouldRun(Description description) {
                            return test1.getMethod().equals(description.getMethodName())
                                    || test2.getMethod().equals(description.getMethodName());
                        }

                        @Override
                        public String describe() {
                            return "Only run polluter and victim";
                        }
                    }).sortWith(new Comparator<Description>() {
                        @Override
                        public int compare(Description des1, Description des2) {
                            if (des1.getMethodName().equals(test1.getMethod())) {
                                return -1;
                            } else {
                                return 1;
                            }
                        }
                    });
                    Result result = junit.run(request);
                    exitCode = printResult(result, test1.toString() + " and " + test2.toString());
                } else {
                    // running separately is fine
                    System.out.println(YELLOW + "Running " + test1 + RESET);
                    Result result = junit.run(Request.method(test1.getTestClass(), test1.getMethod()));
                    exitCode = printResult(result, test1.toString());
                    if (exitCode == 0) {
                        System.out.println(YELLOW + "Running " + test1 + RESET);
                        Result result2 = junit.run(Request.method(test2.getTestClass(), test2.getMethod()));
                        exitCode = printResult(result2, test2.toString());
                    }
                }
            } else {
                System.err.println(RED + "Can't specify more than 2 tests." + RESET);
                exitCode = 1;
            }
            System.exit(exitCode);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void rerunPolluterTest() {

    }

    private static int printResult(Result result, String testDesc) {
        if (result.wasSuccessful()) {
            System.out.println(GREEN + "✓ " + testDesc + " passed" + RESET);
            return 0;
        } else {
            System.out.println(RED + "\u2717 " + testDesc + " failed" + RESET);
            for (Failure failure : result.getFailures()) {
                System.out.println(failure.getTestHeader());
                System.out.println(failure.getMessage() + " " + failure.getDescription());
                failure.getException().printStackTrace();
            }
            return 1;
        }
    }

    private static Class<?>[] getAllTestClasses() {
        List<Class<?>> testClasses = new ArrayList<>();

        try {
            // Discover all test classes dynamically by looking through
            // ./target/test-classes/
            String basePath = "./target/test-classes/";
            findTestClasses(new File(basePath), "", testClasses);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return testClasses.toArray(new Class<?>[0]);
    }

    private static void findTestClasses(File directory, String packageName, List<Class<?>> testClasses) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                if (file.isDirectory()) {
                    String newPackageName = packageName.length() == 0 ? fileName : packageName + "." + fileName;
                    findTestClasses(file, newPackageName, testClasses);
                } else if (fileName.endsWith(".class") && !fileName.contains("$")) {
                    try {
                        String className = packageName + "." + fileName.substring(0, fileName.length() - 6);
                        Class<?> clazz = Class.forName(className);
                        testClasses.add(clazz);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
