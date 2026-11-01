package dev.darkblade.croppy.core.station.component;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.station.component.DisplayComponent;

import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of {@link DisplayComponent}.
 */
public final class DefaultDisplayComponent implements DisplayComponent {

    private volatile Key displayedItem;
    private volatile String statusText = "";
    private volatile float progress = 0.0f;
    private volatile String thermalZone = "NONE";
    private volatile boolean dirty = true;

    @Override
    public Optional<Key> getDisplayedItem() {
        return Optional.ofNullable(displayedItem);
    }

    @Override
    public void setDisplayedItem(Key itemKey) {
        if (!Objects.equals(this.displayedItem, itemKey)) {
            this.displayedItem = itemKey;
            this.dirty = true;
        }
    }

    @Override
    public String getStatusText() {
        return statusText;
    }

    @Override
    public void setStatusText(String text) {
        String clean = (text == null) ? "" : text;
        if (!clean.equals(this.statusText)) {
            this.statusText = clean;
            this.dirty = true;
        }
    }

    @Override
    public float getProgress() {
        return progress;
    }

    @Override
    public void setProgress(float progress) {
        float clamped = Math.max(0.0f, Math.min(1.0f, progress));
        if (Math.abs(this.progress - clamped) > 0.001f) {
            this.progress = clamped;
            this.dirty = true;
        }
    }

    @Override
    public String getThermalZone() {
        return thermalZone;
    }

    @Override
    public void setThermalZone(String zone) {
        String clean = (zone == null) ? "NONE" : zone;
        if (!clean.equalsIgnoreCase(this.thermalZone)) {
            this.thermalZone = clean;
            this.dirty = true;
        }
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
