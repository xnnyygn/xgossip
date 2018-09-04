package in.xnnyygn.xgossip;

import java.util.concurrent.ScheduledFuture;

public class MockScheduler implements Scheduler {

    private Runnable lastCommand;

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay) {
        lastCommand = command;
        return new NullScheduledFuture<>();
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay) {
        return null;
    }

    @Override
    public void shutdown() {

    }

    public void runLastCommand() {
        if (lastCommand != null) {
            lastCommand.run();
        }
    }

}
