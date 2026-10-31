package dev.darkblade.croppy.core.process;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.process.ProcessInstance;
import dev.darkblade.croppy.api.process.ProcessState;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe default implementation of {@link ProcessInstance}.
 */
public final class DefaultProcessInstance implements ProcessInstance {

    private final Key processKey;
    private volatile ProcessState state;
    private volatile int currentPhaseIndex;
    private volatile long phaseElapsedTicks;
    private volatile long totalElapsedTicks;
    private final Map<String, Object> contextVariables = new ConcurrentHashMap<>();

    public DefaultProcessInstance(Key processKey) {
        this(processKey, ProcessState.IDLE, 0, 0, 0);
    }

    public DefaultProcessInstance(Key processKey, ProcessState state, int phaseIndex, long phaseElapsedTicks, long totalElapsedTicks) {
        this.processKey = Objects.requireNonNull(processKey, "processKey cannot be null");
        this.state = Objects.requireNonNull(state, "state cannot be null");
        this.currentPhaseIndex = phaseIndex;
        this.phaseElapsedTicks = phaseElapsedTicks;
        this.totalElapsedTicks = totalElapsedTicks;
    }

    @Override
    public Key getProcessKey() {
        return processKey;
    }

    @Override
    public ProcessState getState() {
        return state;
    }

    @Override
    public void setState(ProcessState state) {
        this.state = Objects.requireNonNull(state, "state cannot be null");
    }

    @Override
    public int getCurrentPhaseIndex() {
        return currentPhaseIndex;
    }

    @Override
    public void setCurrentPhaseIndex(int index) {
        this.currentPhaseIndex = index;
    }

    @Override
    public long getPhaseElapsedTicks() {
        return phaseElapsedTicks;
    }

    @Override
    public void addPhaseElapsedTicks(long ticks) {
        this.phaseElapsedTicks += ticks;
        this.totalElapsedTicks += ticks;
    }

    @Override
    public void resetPhaseElapsedTicks() {
        this.phaseElapsedTicks = 0;
    }

    @Override
    public long getTotalElapsedTicks() {
        return totalElapsedTicks;
    }

    @Override
    public Map<String, Object> getContextVariables() {
        return Collections.unmodifiableMap(contextVariables);
    }

    @Override
    public void setContextVariable(String key, Object value) {
        Objects.requireNonNull(key, "key cannot be null");
        if (value == null) {
            contextVariables.remove(key);
        } else {
            contextVariables.put(key, value);
        }
    }
}
