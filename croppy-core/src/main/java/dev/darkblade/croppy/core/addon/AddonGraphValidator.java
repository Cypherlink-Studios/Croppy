package dev.darkblade.croppy.core.addon;

import dev.darkblade.croppy.api.addon.AddonDescription;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Validates addon dependency graphs and calculates deterministic topological execution order
 * using Kahn's algorithm while detecting cyclic dependencies.
 */
public final class AddonGraphValidator {

    public static final String CORE_DEPENDENCY_ID = "croppy";

    private AddonGraphValidator() {
    }

    /**
     * Resolves the topological enable order of addons such that all dependencies precede their dependents.
     *
     * @param addons collection of addon descriptions to resolve
     * @return topologically sorted list of addons in enable order
     * @throws IllegalStateException if a required dependency is missing or a cycle is detected
     */
    public static List<AddonDescription> resolveOrder(Collection<AddonDescription> addons) {
        Objects.requireNonNull(addons, "addons cannot be null");
        if (addons.isEmpty()) {
            return List.of();
        }

        Map<String, AddonDescription> byId = new HashMap<>();
        for (AddonDescription addon : addons) {
            if (byId.putIfAbsent(addon.id(), addon) != null) {
                throw new IllegalStateException("Duplicate addon identifier in graph: " + addon.id());
            }
        }

        // Graph representations:
        // dependentsMap: dependencyId -> set of addonIds that depend on it (outgoing edges in dependency -> dependent)
        // inDegree: addonId -> count of unresolved incoming dependencies
        Map<String, Set<String>> dependentsMap = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();

        for (String id : byId.keySet()) {
            dependentsMap.put(id, new HashSet<>());
            inDegree.put(id, 0);
        }

        for (AddonDescription addon : addons) {
            String id = addon.id();

            // Validate and record required dependencies
            for (String dep : addon.dependencies()) {
                if (CORE_DEPENDENCY_ID.equalsIgnoreCase(dep)) {
                    continue; // Satisfied by host Croppy core
                }
                if (!byId.containsKey(dep)) {
                    throw new IllegalStateException("Addon '" + id + "' requires missing dependency '" + dep + "'");
                }
                dependentsMap.get(dep).add(id);
                inDegree.put(id, inDegree.get(id) + 1);
            }

            // Record soft dependencies if present
            for (String softDep : addon.softDependencies()) {
                if (CORE_DEPENDENCY_ID.equalsIgnoreCase(softDep)) {
                    continue;
                }
                if (byId.containsKey(softDep)) {
                    dependentsMap.get(softDep).add(id);
                    inDegree.put(id, inDegree.get(id) + 1);
                }
            }
        }

        // Use TreeSet for deterministic alphabetical ordering among nodes with 0 in-degree
        TreeSet<String> zeroInDegreeQueue = new TreeSet<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                zeroInDegreeQueue.add(entry.getKey());
            }
        }

        List<AddonDescription> sorted = new ArrayList<>(addons.size());
        while (!zeroInDegreeQueue.isEmpty()) {
            String currentId = zeroInDegreeQueue.pollFirst();
            sorted.add(byId.get(currentId));

            for (String dependentId : dependentsMap.get(currentId)) {
                int updated = inDegree.get(dependentId) - 1;
                inDegree.put(dependentId, updated);
                if (updated == 0) {
                    zeroInDegreeQueue.add(dependentId);
                }
            }
        }

        if (sorted.size() != addons.size()) {
            List<String> unfulfilled = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
                if (entry.getValue() > 0) {
                    unfulfilled.add(entry.getKey());
                }
            }
            Collections.sort(unfulfilled);
            throw new IllegalStateException("Circular dependency detected among addons: " + unfulfilled);
        }

        return List.copyOf(sorted);
    }
}
