package dev.darkblade.croppy.core.station.component;

import dev.darkblade.croppy.api.process.ProcessInstance;
import dev.darkblade.croppy.api.station.component.ProcessComponent;

import java.util.Optional;

/**
 * Default implementation of {@link ProcessComponent}.
 */
public final class DefaultProcessComponent implements ProcessComponent {

    private volatile ProcessInstance activeProcess;

    public DefaultProcessComponent() {
        this(null);
    }

    public DefaultProcessComponent(ProcessInstance initialProcess) {
        this.activeProcess = initialProcess;
    }

    @Override
    public Optional<ProcessInstance> getActiveProcess() {
        return Optional.ofNullable(activeProcess);
    }

    @Override
    public void setActiveProcess(ProcessInstance process) {
        this.activeProcess = process;
    }

    @Override
    public void clearActiveProcess() {
        this.activeProcess = null;
    }
}
