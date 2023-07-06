package in.natelev.runner;

import java.util.Arrays;

public class TestArg {
    private String method;
    private String className;
    private Class<?> clazz;

    public TestArg(String arg) throws ClassNotFoundException {
        // each arg must be in the form com.example.ClassName.methodName
        String[] parts = arg.split("\\.");
        if (parts.length < 2) {
            System.out.println("Invalid argument: " + arg);
            System.exit(1);
        }
        method = parts[parts.length - 1];
        className = String.join(".", Arrays.copyOfRange(parts, 0, parts.length - 1));
        clazz = Class.forName(className);
    }

    public String getMethod() {
        return method;
    }

    public Class<?> getTestClass() {
        return clazz;
    }

    public String getClassName() {
        return className;
    }

    @Override
    public String toString() {
        return className + "#" + method;
    }
}
