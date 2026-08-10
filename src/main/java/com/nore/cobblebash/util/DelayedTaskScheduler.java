package com.nore.cobblebash.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DelayedTaskScheduler {
    private static final List<ScheduledTask> TASKS = new ArrayList<>();

    public static void schedule(int delayTicks, Runnable action) {
        TASKS.add(new ScheduledTask(delayTicks, action));
    }

    /** Equivalent de {@code ServerTickEvent.Post} cote Fabric. */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> tick());
    }

    private static void tick() {
        Iterator<ScheduledTask> iterator = TASKS.iterator();

        while (iterator.hasNext()) {
            ScheduledTask task = iterator.next();
            task.ticksRemaining--;

            if (task.ticksRemaining <= 0) {
                task.action.run();
                iterator.remove();
            }
        }
    }

    private static class ScheduledTask {
        private int ticksRemaining;
        private final Runnable action;

        private ScheduledTask(int ticksRemaining, Runnable action) {
            this.ticksRemaining = ticksRemaining;
            this.action = action;
        }
    }
}
