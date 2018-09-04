package in.xnnyygn.xgossip;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DefaultScheduler implements Scheduler {

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "scheduler"));

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay) {
        return executorService.schedule(command, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay) {
        return executorService.scheduleWithFixedDelay(command, initialDelay, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutdown() {
        executorService.shutdown();
    }

}
