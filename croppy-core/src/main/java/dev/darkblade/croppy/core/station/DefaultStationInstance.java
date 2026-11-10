package dev.darkblade.croppy.core.station;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.component.StationComponent;
import dev.darkblade.croppy.api.world.BlockPosition;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe default implementation of {@link StationInstance}.
 */
public final class DefaultStationInstance implements StationInstance {

    private final Key stationKey;
    private final BlockPosition position;
    private final StationDefinition definition;
    private final Map<Class<? extends StationComponent>, StationComponent> components = new ConcurrentHashMap<>();
    private volatile float yaw;
    private volatile String activeStateId = "default";

    public DefaultStationInstance(Key stationKey, BlockPosition position, StationDefinition definition, float yaw) {
        this.stationKey = Objects.requireNonNull(stationKey, "stationKey cannot be null");
        this.position = Objects.requireNonNull(position, "position cannot be null");
        this.definition = Objects.requireNonNull(definition, "definition cannot be null");
        this.yaw = yaw;
    }

    public DefaultStationInstance(Key stationKey, BlockPosition position, StationDefinition definition) {
        this(stationKey, position, definition, 0.0f);
    }

    @Override
    public Key getStationKey() {
        return stationKey;
    }

    @Override
    public BlockPosition getPosition() {
        return position;
    }

    @Override
    public StationDefinition getDefinition() {
        return definition;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends StationComponent> Optional<T> getComponent(Class<T> componentType) {
        Objects.requireNonNull(componentType, "componentType cannot be null");
        return Optional.ofNullable((T) components.get(componentType));
    }

    @Override
    public <T extends StationComponent> void setComponent(Class<T> componentType, T component) {
        Objects.requireNonNull(componentType, "componentType cannot be null");
        if (component == null) {
            components.remove(componentType);
        } else {
            components.put(componentType, component);
        }
    }

    @Override
    public Map<Class<? extends StationComponent>, StationComponent> getComponents() {
        return Collections.unmodifiableMap(components);
    }

    @Override
    public float getYaw() {
        return yaw;
    }

    @Override
    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    @Override
    public String getActiveStateId() {
        return activeStateId;
    }

    @Override
    public void setActiveStateId(String activeStateId) {
        this.activeStateId = (activeStateId == null || activeStateId.isBlank()) ? "default" : activeStateId;
    }
}
