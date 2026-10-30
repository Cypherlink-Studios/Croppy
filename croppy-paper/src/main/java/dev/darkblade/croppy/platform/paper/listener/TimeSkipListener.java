package dev.darkblade.croppy.platform.paper.listener;

import dev.darkblade.croppy.platform.paper.watering.DailyTransitionService;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.TimeSkipEvent;

import java.util.Objects;

/**
 * Listens for bed sleep and world time skips to smoothly advance daily crop moisture transitions.
 */
public class TimeSkipListener implements Listener {

    private final DailyTransitionService dailyTransitionService;

    public TimeSkipListener(DailyTransitionService dailyTransitionService) {
        this.dailyTransitionService = Objects.requireNonNull(dailyTransitionService, "dailyTransitionService cannot be null");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTimeSkip(TimeSkipEvent event) {
        World world = event.getWorld();
        long oldDay = world.getFullTime() / 24000L;
        long newDay = (world.getFullTime() + event.getSkipAmount()) / 24000L;

        if (newDay > oldDay) {
            dailyTransitionService.handleTransition(world, oldDay, newDay);
        }
    }
}
