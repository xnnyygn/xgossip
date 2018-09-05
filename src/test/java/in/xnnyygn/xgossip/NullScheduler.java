package in.xnnyygn.xgossip;

import in.xnnyygn.xgossip.schedule.Scheduler;

import java.util.concurrent.ScheduledFuture;

public class NullScheduler implements Scheduler {

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay) {
        return new NullScheduledFuture<>();
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay) {
        return new NullScheduledFuture<>();
    }

    @Override
    public void shutdown() {
    }

}
