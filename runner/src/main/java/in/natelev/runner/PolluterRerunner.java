package in.natelev.runner;

import java.lang.reflect.Method;

import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;

public class PolluterRerunner {
    private Method runChildMethod;
    private BlockJUnit4ClassRunner runner;
    private String polluterMethodName;

    private static boolean shouldRunPolluterNext = false;

    PolluterRerunner(Request polluterRequest, JUnitCore junit, String polluterMethodName) {
        runner = (BlockJUnit4ClassRunner) polluterRequest.getRunner();
        this.polluterMethodName = polluterMethodName;

        try {
            runChildMethod = BlockJUnit4ClassRunner.class.getDeclaredMethod("runChild",
                    FrameworkMethod.class, RunNotifier.class);
            runChildMethod.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        junit.addListener(new RunListener() {
            @Override
            public void testFinished(Description description) throws Exception {
                if (shouldRunPolluterNext) {
                    shouldRunPolluterNext = false;
                    rerunPolluterImmediately();
                }
            }
        });
    }

    public void rerunPolluter() {
        shouldRunPolluterNext = true;
    }

    private void rerunPolluterImmediately() {
        System.out.println("Rerunning: " + runner.getTestClass().getJavaClass().getName() + "." + polluterMethodName);
        try {
            runChildMethod.invoke(runner,
                    new FrameworkMethod(
                            runner.getTestClass().getJavaClass().getDeclaredMethod(polluterMethodName)),
                    new RunNotifier());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
