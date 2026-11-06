package dev.darkblade.croppy.core.addon;

import dev.darkblade.croppy.api.addon.AddonDescription;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AddonGraphValidatorTest {

    private AddonDescription createAddon(String id, List<String> deps, List<String> softDeps) {
        return new AddonDescription(
                id,
                id.toUpperCase(),
                "1.0.0",
                id.replace("-", "_"),
                Optional.empty(),
                List.of("Author"),
                "Description",
                deps,
                softDeps
        );
    }

    @Test
    @DisplayName("Resolve linear dependency chain [A, B, C]")
    void testLinearDependencyChain() {
        AddonDescription addonA = createAddon("addon-a", List.of("croppy"), List.of());
        AddonDescription addonB = createAddon("addon-b", List.of("addon-a"), List.of());
        AddonDescription addonC = createAddon("addon-c", List.of("addon-b"), List.of());

        List<AddonDescription> order = AddonGraphValidator.resolveOrder(List.of(addonC, addonA, addonB));
        assertEquals(3, order.size());
        assertEquals("addon-a", order.get(0).id());
        assertEquals("addon-b", order.get(1).id());
        assertEquals("addon-c", order.get(2).id());
    }

    @Test
    @DisplayName("Deterministic alphabetical order for independent addons")
    void testIndependentAddons() {
        AddonDescription beta = createAddon("beta", List.of(), List.of());
        AddonDescription alpha = createAddon("alpha", List.of(), List.of());
        AddonDescription gamma = createAddon("gamma", List.of(), List.of());

        List<AddonDescription> order = AddonGraphValidator.resolveOrder(List.of(gamma, beta, alpha));
        assertEquals(List.of("alpha", "beta", "gamma"), order.stream().map(AddonDescription::id).toList());
    }

    @Test
    @DisplayName("Detect circular dependency and throw IllegalStateException")
    void testCircularDependency() {
        AddonDescription addonA = createAddon("addon-a", List.of("addon-b"), List.of());
        AddonDescription addonB = createAddon("addon-b", List.of("addon-a"), List.of());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                AddonGraphValidator.resolveOrder(List.of(addonA, addonB)));
        assertTrue(ex.getMessage().contains("Circular dependency"));
    }

    @Test
    @DisplayName("Missing required dependency throws informative IllegalStateException")
    void testMissingDependency() {
        AddonDescription addonA = createAddon("addon-a", List.of("missing-lib"), List.of());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                AddonGraphValidator.resolveOrder(List.of(addonA)));
        assertTrue(ex.getMessage().contains("missing dependency 'missing-lib'"));
    }

    @Test
    @DisplayName("Soft dependency orders before if present, succeeds if absent")
    void testSoftDependency() {
        // When soft dep is present
        AddonDescription mainAddon = createAddon("main", List.of(), List.of("optional-addon"));
        AddonDescription optionalAddon = createAddon("optional-addon", List.of(), List.of());

        List<AddonDescription> order = AddonGraphValidator.resolveOrder(List.of(mainAddon, optionalAddon));
        assertEquals("optional-addon", order.get(0).id());
        assertEquals("main", order.get(1).id());

        // When soft dep is absent
        List<AddonDescription> orderWithoutSoft = AddonGraphValidator.resolveOrder(List.of(mainAddon));
        assertEquals(1, orderWithoutSoft.size());
        assertEquals("main", orderWithoutSoft.get(0).id());
    }
}
