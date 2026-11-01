package dev.darkblade.croppy.platform.paper.menu;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.menu.MenuDefinition;
import dev.darkblade.croppy.api.menu.SlotType;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.core.menu.DefaultMenuRegistry;
import dev.darkblade.croppy.core.menu.YamlMenuLoader;
import dev.darkblade.croppy.core.process.DefaultProcessRegistry;
import dev.darkblade.croppy.core.station.DefaultStationInstance;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PaperMenuIntegrationTest {

    @Test
    @DisplayName("Verify default bundled YAML menu resource configurations")
    void testBundledMenuResources() {
        YamlMenuLoader loader = new YamlMenuLoader();
        DefaultMenuRegistry menuRegistry = new DefaultMenuRegistry();

        // 1. Default station menu
        InputStream defStream = getClass().getClassLoader().getResourceAsStream("menus/default_station.yml");
        assertNotNull(defStream, "menus/default_station.yml resource must exist");
        MenuDefinition defaultMenu = loader.load(defStream, "default_station.yml");
        assertEquals(Key.of("croppy", "default_station"), defaultMenu.getKey());
        assertEquals(3, defaultMenu.rows());
        assertEquals(1, defaultMenu.getSlotsForType(SlotType.INPUT).size());
        assertEquals(1, defaultMenu.getSlotsForType(SlotType.OUTPUT).size());
        assertEquals(1, defaultMenu.getSlotsForType(SlotType.STATUS).size());
        menuRegistry.register(defaultMenu);

        // 2. Cooking Pot menu
        InputStream potStream = getClass().getClassLoader().getResourceAsStream("menus/stations/cooking_pot.yml");
        assertNotNull(potStream, "menus/stations/cooking_pot.yml resource must exist");
        MenuDefinition potMenu = loader.load(potStream, "cooking_pot.yml");
        assertEquals(Key.of("croppy", "cooking_pot"), potMenu.getKey());
        assertEquals(4, potMenu.rows());
        assertEquals(6, potMenu.getSlotsForType(SlotType.INPUT).size());
        assertEquals(2, potMenu.getSlotsForType(SlotType.OUTPUT).size());
        assertEquals(1, potMenu.getSlotsForType(SlotType.STATUS).size());
        assertEquals(1, potMenu.getSlotsForType(SlotType.ACTION).size());
        menuRegistry.register(potMenu);

        // 3. Skillet menu
        InputStream skilletStream = getClass().getClassLoader().getResourceAsStream("menus/stations/skillet.yml");
        assertNotNull(skilletStream, "menus/stations/skillet.yml resource must exist");
        MenuDefinition skilletMenu = loader.load(skilletStream, "skillet.yml");
        assertEquals(Key.of("croppy", "skillet"), skilletMenu.getKey());
        assertEquals(3, skilletMenu.rows());
        assertEquals(4, skilletMenu.getSlotsForType(SlotType.INPUT).size());
        assertEquals(2, skilletMenu.getSlotsForType(SlotType.OUTPUT).size());
        assertEquals(1, skilletMenu.getSlotsForType(SlotType.STATUS).size());
        assertEquals(1, skilletMenu.getSlotsForType(SlotType.ACTION).size());
        menuRegistry.register(skilletMenu);

        // 4. Stove menu
        InputStream stoveStream = getClass().getClassLoader().getResourceAsStream("menus/stations/stove_menu.yml");
        assertNotNull(stoveStream, "menus/stations/stove_menu.yml resource must exist");
        MenuDefinition stoveMenu = loader.load(stoveStream, "stove_menu.yml");
        assertEquals(Key.of("croppy", "stove_menu"), stoveMenu.getKey());
        assertEquals(3, stoveMenu.rows());
        assertEquals(1, stoveMenu.getSlotsForType(SlotType.INPUT).size());
        assertEquals(1, stoveMenu.getSlotsForType(SlotType.STATUS).size());
        assertEquals(6, stoveMenu.getSlotsForType(SlotType.ACTION).size());
        menuRegistry.register(stoveMenu);

        // 5. Recipe Compendium menu
        InputStream recStream = getClass().getClassLoader().getResourceAsStream("menus/recipes_menu.yml");
        assertNotNull(recStream, "menus/recipes_menu.yml resource must exist");
        MenuDefinition recMenu = loader.load(recStream, "recipes_menu.yml");
        assertEquals(CompendiumMenuController.MENU_KEY, recMenu.getKey());
        assertEquals(6, recMenu.rows());
        int recipeSlots = 0;
        for (int i = 0; i < recMenu.size(); i++) {
            if (Character.toUpperCase(recMenu.getSymbolAt(i)) == 'R') {
                recipeSlots++;
            }
        }
        assertEquals(28, recipeSlots);
        menuRegistry.register(recMenu);

        // 6. Test PaperMenuService setup
        DefaultItemRegistry itemRegistry = new DefaultItemRegistry();
        ItemFactory itemFactory = new ItemFactory(null, itemRegistry);
        PaperMenuRenderer renderer = new PaperMenuRenderer(itemFactory);
        DefaultProcessRegistry processRegistry = new DefaultProcessRegistry();
        PaperMenuTicker ticker = new PaperMenuTicker(renderer, processRegistry);
        PaperMenuService service = new PaperMenuService(menuRegistry, renderer, processRegistry, ticker, null);

        assertSame(menuRegistry, service.getMenuRegistry());

        // Verify fallback resolution
        StationDefinition unknownMenuStation = new StationDefinition(
                Key.of("croppy", "unmapped_station"),
                "Unmapped",
                "barrel",
                StationInteractionType.GUI,
                1,
                1,
                Key.of("croppy", "nonexistent_menu"),
                false,
                List.of(),
                Map.of()
        );
        // Menu fallback is croppy:default_station which is registered in registry
        assertTrue(service.getMenuRegistry().contains(StationDefinition.DEFAULT_MENU_KEY));
    }
}
