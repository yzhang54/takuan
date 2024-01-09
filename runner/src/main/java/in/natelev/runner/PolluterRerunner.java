package in.natelev.runner;

import java.lang.reflect.Method;

import org.junit.runner.Request;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;

public class PolluterRerunner {
    private Method runChildMethod;
    private BlockJUnit4ClassRunner runner;

    PolluterRerunner(Request polluterRequest) {
        runner = (BlockJUnit4ClassRunner) polluterRequest.getRunner();

        try {
            runChildMethod = BlockJUnit4ClassRunner.class.getDeclaredMethod("runChild",
                    FrameworkMethod.class, RunNotifier.class);
            runChildMethod.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void rerunPolluter() {
        System.out.println("Rerunning: " + runner.getTestClass().getJavaClass().getName());
        try {
            runChildMethod.invoke(runner,
                    new FrameworkMethod(
                            runner.getTestClass().getJavaClass().getDeclaredMethod("testStringLog")),
                    new RunNotifier());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
