package dev.darkblade.croppy.platform.paper.menu;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.process.PhaseDefinition;
import dev.darkblade.croppy.api.process.ProcessDefinition;
import dev.darkblade.croppy.api.process.ProcessInstance;
import dev.darkblade.croppy.api.process.ProcessRegistry;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.component.HeatSourceComponent;
import dev.darkblade.croppy.api.station.component.ProcessComponent;
import dev.darkblade.croppy.api.station.component.ThermalComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Objects;
import java.util.Optional;

/**
 * Lightweight periodic task that refreshes dynamic slots (status icons, progress lore)
 * on active {@link PaperMenuHolder} inventories without cursor resets.
 */
public final class PaperMenuTicker implements Runnable {

    private final PaperMenuRenderer renderer;
    private final ProcessRegistry processRegistry;

    public PaperMenuTicker(PaperMenuRenderer renderer, ProcessRegistry processRegistry) {
        this.renderer = Objects.requireNonNull(renderer, "renderer cannot be null");
        this.processRegistry = processRegistry; // may be null in tests
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Inventory top = player.getOpenInventory().getTopInventory();
            if (!(top.getHolder() instanceof PaperMenuHolder holder)) {
                continue;
            }

            // If station is present, enrich context and refresh
            if (holder.getStation().isPresent()) {
                StationInstance station = holder.getStation().get();
                EvaluationContext enriched = enrichContext(holder.getContext(), station, player);
                holder.setContext(enriched);
                renderer.populateOutputContents(holder);
            }

            renderer.refreshDynamicSlots(holder);
        }
    }

    /**
     * Enriches the evaluation context with live station, thermal, and process progress variables.
     *
     * @param base    base context
     * @param station station instance
     * @param player  viewing player
     * @return enriched context
     */
    public EvaluationContext enrichContext(EvaluationContext base, StationInstance station, Player player) {
        EvaluationContext ctx = (base == null) ? EvaluationContext.empty() : base;
        if (player != null) {
            ctx = ctx.with("player", player);
        }
        if (station == null) {
            return ctx;
        }

        ctx = ctx.with("station.id", station.getDefinition().getKey().asString())
                .with("station.name", station.getDefinition().displayName());

        Optional<ThermalComponent> thermalOpt = station.getComponent(ThermalComponent.class);
        if (thermalOpt.isPresent()) {
            ThermalComponent tc = thermalOpt.get();
            ctx = ctx.with("station.temperature", (long) tc.getTemperature())
                    .with("station.heat_level", (long) tc.getTemperature())
                    .with("station.is_heated", tc.isHeated())
                    .with("station.thermal_zone", tc.getThermalZone().name());
        }

        Optional<HeatSourceComponent> heatOpt = station.getComponent(HeatSourceComponent.class);
        if (heatOpt.isPresent()) {
            HeatSourceComponent hs = heatOpt.get();
            ctx = ctx.with("station.is_active", hs.isActive())
                    .with("station.is_burning", hs.isBurning())
                    .with("station.target_temperature", (long) hs.getTargetTemperature())
                    .with("station.output_temperature", (long) hs.getOutputTemperature())
                    .with("station.burn_ticks", hs.getBurnTicksRemaining())
                    .with("station.burn_seconds", hs.getBurnTicksRemaining() / 20)
                    .with("station.min_temperature", (long) hs.getMinTemperature())
                    .with("station.max_temperature", (long) hs.getMaxTemperature());
            if (thermalOpt.isEmpty()) {
                ctx = ctx.with("station.temperature", (long) hs.getOutputTemperature())
                        .with("station.heat_level", (long) hs.getOutputTemperature())
                        .with("station.is_heated", hs.isActive());
            }
        }

        Optional<ProcessComponent> processOpt = station.getComponent(ProcessComponent.class);
        if (processOpt.isPresent() && processOpt.get().hasActiveProcess()) {
            ProcessInstance pi = processOpt.get().getActiveProcess().get();
            ctx = ctx.with("process.state", pi.getState().name());

            if (processRegistry != null) {
                Optional<ProcessDefinition> pDefOpt = processRegistry.get(pi.getProcessKey());
                if (pDefOpt.isPresent()) {
                    ProcessDefinition pDef = pDefOpt.get();
                    ctx = ctx.with("recipe.name", pDef.displayName());
                    int phaseIdx = pi.getCurrentPhaseIndex();
                    if (phaseIdx >= 0 && phaseIdx < pDef.phases().size()) {
                        PhaseDefinition phase = pDef.phases().get(phaseIdx);
                        long duration = phase.durationTicks();
                        long elapsed = pi.getPhaseElapsedTicks();
                        int percent = (int) Math.min(100, Math.max(0, (elapsed * 100) / Math.max(1, duration)));
                        long remainingSeconds = Math.max(0, (duration - elapsed) / 20);
                        ctx = ctx.with("process.progress_percent", percent)
                                .with("process.remaining_seconds", remainingSeconds);
                    }
                }
            }
        } else {
            ctx = ctx.with("process.state", "IDLE");
        }

        return ctx;
    }
}
