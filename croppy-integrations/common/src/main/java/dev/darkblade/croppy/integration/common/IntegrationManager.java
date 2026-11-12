package dev.darkblade.croppy.integration.common;

import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.JulCroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;

import java.util.*;
import java.util.logging.Logger;

/**
 * Coordinates registration, safe dependency checks, and lifecycle dispatch
 * (onLoad, onEnable, onDisable) across all external plugin integrations.
 */
public class IntegrationManager {

    private final CroppyLogger logger;
    private final Map<String, CroppyIntegration> integrations = new LinkedHashMap<>();
    private final Set<String> activeIntegrations = new HashSet<>();

    public IntegrationManager(CroppyLogger logger) {
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
    }

    public IntegrationManager(Logger logger) {
        this(new JulCroppyLogger(Objects.requireNonNull(logger, "logger cannot be null")));
    }

    /**
     * Registers an integration candidate.
     *
     * @param integration the integration instance (cannot be null)
     */
    public void register(CroppyIntegration integration) {
        Objects.requireNonNull(integration, "integration cannot be null");
        integrations.put(integration.id().toLowerCase(Locale.ROOT), integration);
    }

    /**
     * Retrieves an integration by its unique identifier.
     *
     * @param id the integration identifier
     * @return Optional holding the integration if registered
     */
    public Optional<CroppyIntegration> getIntegration(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(integrations.get(id.toLowerCase(Locale.ROOT)));
    }

    /**
     * Returns an unmodifiable collection of all registered integrations.
     *
     * @return unmodifiable collection of integrations
     */
    public Collection<CroppyIntegration> getAllIntegrations() {
        return Collections.unmodifiableCollection(integrations.values());
    }

    /**
     * Checks if a given integration is currently enabled and active.
     *
     * @param id the integration identifier
     * @return true if currently enabled
     */
    public boolean isActive(String id) {
        if (id == null) {
            return false;
        }
        return activeIntegrations.contains(id.toLowerCase(Locale.ROOT));
    }

    /**
     * Dispatches the onLoad lifecycle event to all available integrations.
     */
    public void onLoad() {
        for (CroppyIntegration integration : integrations.values()) {
            try {
                if (integration.isAvailable()) {
                    integration.onLoad();
                }
            } catch (Throwable t) {
                logger.error(LogChannel.INTEGRATION, "Failed during onLoad for integration: " + integration.id(), t);
            }
        }
    }

    /**
     * Dispatches the onEnable lifecycle event to all available integrations.
     */
    public void onEnable() {
        for (CroppyIntegration integration : integrations.values()) {
            try {
                if (integration.isAvailable()) {
                    integration.onEnable();
                    activeIntegrations.add(integration.id().toLowerCase(Locale.ROOT));
                    logger.debug(LogChannel.INTEGRATION, () -> "Successfully hooked integration: " + integration.id());
                }
            } catch (Throwable t) {
                logger.error(LogChannel.INTEGRATION, "Failed during onEnable for integration: " + integration.id(), t);
            }
        }
    }

    /**
     * Dispatches the onDisable lifecycle event to all currently active integrations.
     */
    public void onDisable() {
        for (CroppyIntegration integration : integrations.values()) {
            if (activeIntegrations.contains(integration.id().toLowerCase(Locale.ROOT))) {
                try {
                    integration.onDisable();
                } catch (Throwable t) {
                    logger.error(LogChannel.INTEGRATION, "Failed during onDisable for integration: " + integration.id(), t);
                }
            }
        }
        activeIntegrations.clear();
    }
}
