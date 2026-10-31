package dev.darkblade.croppy.platform.paper.menu;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.menu.MenuDefinition;
import dev.darkblade.croppy.api.menu.SlotDefinition;
import dev.darkblade.croppy.api.menu.SlotType;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.core.station.DefaultStationInstance;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PaperMenuRendererTest {

    @Test
    @DisplayName("replacePlaceholders correctly substitutes context variables")
    void testReplacePlaceholders() {
        EvaluationContext ctx = EvaluationContext.empty()
                .with("recipe.name", "Veggie Soup")
                .with("progress", 85)
                .with("heat", 120);

        String template = "<yellow>Recipe:</yellow> <white>{recipe.name}</white> ({progress}%) Heat: {heat}°C";
        String resolved = PaperMenuRenderer.replacePlaceholders(template, ctx);

        assertEquals("<yellow>Recipe:</yellow> <white>Veggie Soup</white> (85%) Heat: 120°C", resolved);

        // Missing variable should resolve to empty string
        String withMissing = "Missing: {nonexistent}!";
        assertEquals("Missing: !", PaperMenuRenderer.replacePlaceholders(withMissing, ctx));
    }

    @Test
    @DisplayName("resolveMaterial handles prefixes and fallbacks")
    void testResolveMaterial() {
        DefaultItemRegistry itemRegistry = new DefaultItemRegistry();
        ItemFactory itemFactory = new ItemFactory(null, itemRegistry);
        PaperMenuRenderer renderer = new PaperMenuRenderer(itemFactory);

        assertEquals(Material.STONE, renderer.resolveMaterial("minecraft:stone"));
        assertEquals(Material.BARREL, renderer.resolveMaterial("barrel"));
        assertEquals(Material.COMPASS, renderer.resolveMaterial("MINECRAFT:COMPASS"));
        // Invalid material falls back to STONE
        assertEquals(Material.STONE, renderer.resolveMaterial("invalid_material_xyz"));
        // Null / blank falls back to BARRIER
        assertEquals(Material.BARRIER, renderer.resolveMaterial(null));
        assertEquals(Material.BARRIER, renderer.resolveMaterial("   "));
    }

    @Test
    @DisplayName("PaperMenuHolder links MenuDefinition, StationInstance, and EvaluationContext")
    void testPaperMenuHolder() {
        Key key = Key.of("croppy", "test_menu");
        MenuDefinition menu = new MenuDefinition(
                key,
                "Title",
                1,
                List.of("X I S O X X X X X".replace(" ", "")),
                Map.of(
                        'X', SlotDefinition.decoration(null),
                        'I', SlotDefinition.input(),
                        'S', new SlotDefinition(SlotType.STATUS, null, List.of(), List.of(), Map.of()),
                        'O', SlotDefinition.output()
                ),
                Map.of()
        );

        StationDefinition stationDef = new StationDefinition(
                Key.of("croppy", "test_station"),
                "Test Station",
                "minecraft:barrel",
                StationInteractionType.GUI,
                1,
                1,
                false,
                List.of(),
                Map.of()
        );
        DefaultStationInstance station = new DefaultStationInstance(stationDef.getKey(), new BlockPosition("world", 10, 64, 10), stationDef);
        EvaluationContext context = EvaluationContext.empty().with("key", "val");

        PaperMenuHolder holder = new PaperMenuHolder(menu, station, context);

        assertEquals(menu, holder.getMenu());
        assertTrue(holder.getStation().isPresent());
        assertSame(station, holder.getStation().get());
        assertEquals("val", holder.getContext().getVariable("key").orElse(null));

        holder.setContext(context.with("key", "new_val"));
        assertEquals("new_val", holder.getContext().getVariable("key").orElse(null));
    }
}
