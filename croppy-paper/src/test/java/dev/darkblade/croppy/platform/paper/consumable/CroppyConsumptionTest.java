package dev.darkblade.croppy.platform.paper.consumable;

import dev.darkblade.croppy.api.effect.ActiveStatusEffect;
import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ConsumableProperties;
import dev.darkblade.croppy.api.item.ConsumeEffectSpec;
import dev.darkblade.croppy.api.item.FoodProperties;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.api.item.QualityScaling;
import dev.darkblade.croppy.api.item.UseAnimation;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import dev.darkblade.croppy.core.CroppyCore;
import dev.darkblade.croppy.core.consumable.ConsumptionEvaluator;
import dev.darkblade.croppy.core.consumable.SimpleConsumeExecutionContext;
import dev.darkblade.croppy.core.item.YamlItemLoader;
import dev.darkblade.croppy.platform.paper.config.PluginConfig;
import dev.darkblade.croppy.platform.paper.effect.AlcoholChatListener;
import dev.darkblade.croppy.platform.paper.effect.PaperHangoverEffect;
import dev.darkblade.croppy.platform.paper.effect.PaperIntoxicationEffect;
import dev.darkblade.croppy.platform.paper.effect.PdcPlayerEffectStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CroppyConsumptionTest {

    private CroppyCore core;
    private PaperIntoxicationEffect intoxication;
    private PaperHangoverEffect hangover;
    private ConsumptionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        core = new CroppyCore(NoOpCroppyLogger.INSTANCE);
        core.initialize();
        PluginConfig config = new PluginConfig();
        intoxication = new PaperIntoxicationEffect(config, null);
        hangover = new PaperHangoverEffect(null);
        core.getStatusEffectRegistry().register(intoxication);
        core.getStatusEffectRegistry().register(hangover);
        BuiltInConsumeEffectHandlers.registerAll(core.getConsumeEffectRegistry(), core.getPlayerEffectService(), NoOpCroppyLogger.INSTANCE);
        evaluator = new ConsumptionEvaluator(core.getConsumeEffectRegistry(), NoOpCroppyLogger.INSTANCE);
    }

    @Test
    void testAlcoholConsumeEffectProgressionHangoverAndReduceStatus() {
        UUID playerId = UUID.randomUUID();
        ConsumableProperties wineProps = new ConsumableProperties(
                new FoodProperties(2, 2.0f, true, QualityScaling.NONE),
                1.6f, UseAnimation.DRINK, null, true, null, null,
                List.of(new ConsumeEffectSpec(Key.of("croppy", "alcohol"), Map.of("abv", 12.0, "volume-ml", 250.0)))
        );
        ItemDefinition wine = new ItemDefinition(
                Key.of("croppy", "wine"), "drinks", "POTION", 0, null, null, "Wine", List.of(), Map.of(), wineProps);

        SimpleConsumeExecutionContext ctx = new SimpleConsumeExecutionContext(
                playerId, "Player", wine, null, 0, EvaluationContext.empty(), null);

        // 1 glass = 24.0 -> Tipsy
        evaluator.execute(wineProps, ctx);
        assertEquals("tipsy", core.getPlayerEffectService().getCurrentStage(playerId, PaperIntoxicationEffect.KEY).orElseThrow().id());

        // 2 more glasses = 72.0 -> Heavily Drunk (>= 70 hangover threshold)
        evaluator.execute(wineProps, ctx);
        evaluator.execute(wineProps, ctx);
        assertEquals("heavily_drunk", core.getPlayerEffectService().getCurrentStage(playerId, PaperIntoxicationEffect.KEY).orElseThrow().id());

        // Coffee antidote reduces intoxication by 25.0 -> 47.0 (Drunk)
        ConsumableProperties coffeeProps = new ConsumableProperties(
                null, 0.8f, UseAnimation.DRINK, null, false, null, null,
                List.of(new ConsumeEffectSpec(Key.of("croppy", "reduce_status"),
                        Map.of("status", "croppy:intoxication", "intensity", 25.0)))
        );
        ItemDefinition coffee = new ItemDefinition(
                Key.of("croppy", "coffee"), "drinks", "POTION", 0, null, null, "Coffee", List.of(), Map.of(), coffeeProps);
        evaluator.execute(coffeeProps, new SimpleConsumeExecutionContext(
                playerId, "Player", coffee, null, 0, EvaluationContext.empty(), null));
        assertEquals("drunk", core.getPlayerEffectService().getCurrentStage(playerId, PaperIntoxicationEffect.KEY).orElseThrow().id());

        // Decay down to 0 -> triggers hangover because peak was >= 70.0
        core.getPlayerEffectService().tick(20L * 200L);
        assertFalse(core.getPlayerEffectService().hasEffect(playerId, PaperIntoxicationEffect.KEY));
        assertTrue(core.getPlayerEffectService().hasEffect(playerId, PaperHangoverEffect.KEY));
    }

    @Test
    void testPdcBinaryRoundTrip() {
        PdcPlayerEffectStorage storage = new PdcPlayerEffectStorage(NoOpCroppyLogger.INSTANCE);
        List<ActiveStatusEffect> input = List.of(
                new ActiveStatusEffect(PaperIntoxicationEffect.KEY, 1, 55.5, 4440L, 4440L, 1000L, 2000L, Map.of()),
                new ActiveStatusEffect(PaperHangoverEffect.KEY, 0, 0.0, 1200L, 1200L, 3000L, 4000L, Map.of())
        );

        byte[] encoded = storage.encode(input);
        List<ActiveStatusEffect> decoded = storage.decode(encoded);
        assertEquals(2, decoded.size());
        assertEquals(PaperIntoxicationEffect.KEY, decoded.get(0).effectKey());
        assertEquals(55.5, decoded.get(0).intensity(), 0.001);
        assertEquals(PaperHangoverEffect.KEY, decoded.get(1).effectKey());
        assertEquals(1200L, decoded.get(1).remainingTicks());
    }

    @Test
    void testAlcoholSlurMessageAndStarterFoodsParseCleanly() {
        Random alwaysTrigger = new Random() {
            @Override
            public double nextDouble() {
                return 0.0;
            }
        };
        String slurred = AlcoholChatListener.slurMessage("So many grapes", 80.0, alwaysTrigger);
        assertTrue(slurred.contains("Sh") && slurred.contains("*hic*"), "Expected slurred output: " + slurred);

        YamlItemLoader loader = new YamlItemLoader();
        for (String path : List.of(
                "items/ingredients/fried_egg.yml",
                "items/ingredients/tomato_soup.yml",
                "items/ingredients/vegetable_stew.yml",
                "items/ingredients/tomato.yml"
        )) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
                assertNotNull(in, "Missing starter resource: " + path);
                ItemDefinition def = loader.load(in, "ingredients", path);
                assertTrue(def.getConsumableProperties().isPresent(), path + " should define consumableProperties");
                assertTrue(def.getConsumableProperties().get().getFood().isPresent(), path + " should define food");
            } catch (Exception e) {
                fail("Failed loading " + path + ": " + e.getMessage());
            }
        }
    }
}
