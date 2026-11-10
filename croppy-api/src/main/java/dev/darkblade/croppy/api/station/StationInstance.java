package dev.darkblade.croppy.api.station;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.station.component.StationComponent;
import dev.darkblade.croppy.api.world.BlockPosition;

import java.util.Map;
import java.util.Optional;

/**
 * Runtime state instance of a crafting station located at a discrete spatial position.
 */
public interface StationInstance {

    /**
     * @return unique key identifying the station definition
     */
    Key getStationKey();

    /**
     * @return world block position of this station
     */
    BlockPosition getPosition();

    /**
     * @return definition spec of this station
     */
    StationDefinition getDefinition();

    /**
     * Retrieves an attached component by its type.
     *
     * @param componentType class of the component
     * @param <T>           component type
     * @return optional containing the component if present
     */
    <T extends StationComponent> Optional<T> getComponent(Class<T> componentType);

    /**
     * Attaches or replaces a component.
     *
     * @param componentType class of the component
     * @param component     component instance
     * @param <T>           component type
     */
    <T extends StationComponent> void setComponent(Class<T> componentType, T component);

    /**
     * @return unmodifiable view of all attached components
     */
    Map<Class<? extends StationComponent>, StationComponent> getComponents();

    /**
     * @return the horizontal yaw orientation in degrees (0-360) of this station instance
     */
    default float getYaw() {
        return 0.0f;
    }

    /**
     * Sets the horizontal yaw orientation in degrees of this station instance.
     *
     * @param yaw the yaw angle in degrees
     */
    default void setYaw(float yaw) {
    }

    /**
     * @return the resolved active state identifier of this station (e.g. "lit", "boiling", "default")
     */
    default String getActiveStateId() {
        return "default";
    }

    /**
     * Sets the resolved active state identifier of this station.
     *
     * @param stateId active state identifier
     */
    default void setActiveStateId(String stateId) {
    }
}
