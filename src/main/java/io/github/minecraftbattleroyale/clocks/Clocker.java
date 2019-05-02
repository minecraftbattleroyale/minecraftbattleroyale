package io.github.minecraftbattleroyale.clocks;

import org.spongepowered.api.scheduler.SpongeExecutorService;

import java.util.concurrent.TimeUnit;

public abstract class Clocker {
    protected long time;
    protected TimeUnit timeUnit;
    protected Clock clock;

    /** Run a clock for x amount of time after x amount of time */
    public Clocker(long time,  TimeUnit unit) {
        this.time = time;
        this.timeUnit = unit;
    }

    public SpongeExecutorService.SpongeFuture run(SpongeExecutorService scheduler) {
        clock = new Clock(time, timeUnit);
        clock.task = scheduler.scheduleAtFixedRate(clock, 0, 250, TimeUnit.MILLISECONDS); // run 20 times a second
        return clock.task;
    }

    /** Simple math formula to convert ticks to secs. */
    public int sec(int ticks) {
        return ticks / 20 + 1;
    }

    public long getTime() {
        return clock.endded;
    }

    /** Code to ran each clock tock */
    public void runFirst(long position) {}

    /** Code to ran each clock tock */
    public abstract void runTock(long position);

    /** Code to be ran on the last clock tick */
    public void runLast(long position) {}


    public class Clock implements Runnable {
        public SpongeExecutorService.SpongeFuture task;

        private long started = System.currentTimeMillis();
        private long endded;
        private boolean hasStarted = false;

        protected Clock(long elapse, TimeUnit unit) {
            endded = started + unit.toMillis(elapse);
        }

        @Override
        public void run() {
            long now = System.currentTimeMillis();
            if (!hasStarted) {
                runFirst(now);
                hasStarted = true;
            }
            runTock(now);
            // After stage should we cancel
            if (System.currentTimeMillis() > endded) {
                runLast(now);
                task.cancel(false);
            }
        }
    }
}
