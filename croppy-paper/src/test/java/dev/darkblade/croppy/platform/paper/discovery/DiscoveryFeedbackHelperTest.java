package dev.darkblade.croppy.platform.paper.discovery;

import dev.darkblade.croppy.api.i18n.LocaleRef;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import dev.darkblade.croppy.api.recipe.HintDefinition;
import dev.darkblade.croppy.core.i18n.DefaultMessageCatalog;
import dev.darkblade.croppy.platform.paper.i18n.PaperMessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DiscoveryFeedbackHelperTest {

    private final PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private Player mockPlayer(AtomicReference<Component> capturedMessage) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if ("sendMessage".equals(method.getName()) && args != null && args.length >= 1 && args[0] instanceof Component c) {
                        capturedMessage.set(c);
                        return null;
                    }
                    if ("locale".equals(method.getName())) {
                        return Locale.US;
                    }
                    return null;
                }
        );
    }

    @Test
    void testPlayHintFeedbackWithMessageServiceParsesMiniMessageTags() {
        DefaultMessageCatalog catalog = new DefaultMessageCatalog(LocaleRef.EN_US, LocaleRef.EN_US);
        catalog.setCatalogs(Map.of(
                LocaleRef.EN_US, Map.of(
                        "compendium.gui.clue-title", "<aqua><bold>Culinary Clue:</bold></aqua>",
                        "compendium.gui.clue-hint", "<gray><hint></gray>"
                )
        ));
        PaperMessageService messageService = new PaperMessageService(catalog, NoOpCroppyLogger.INSTANCE);
        DiscoveryFeedbackHelper helper = new DiscoveryFeedbackHelper(messageService);

        AtomicReference<Component> sent = new AtomicReference<>();
        Player player = mockPlayer(sent);

        helper.playHintFeedback(player, "<gray>Try adding <aqua>fresh herbs</aqua>!</gray>");
        Component styledSent = sent.get();
        assertNotNull(styledSent);
        assertEquals("Culinary Clue: Try adding fresh herbs!", plain.serialize(styledSent));
        String serialized = miniMessage.serialize(styledSent);
        assertFalse(serialized.contains("\\<gray>") || serialized.contains("\\<aqua>"),
                "Hint feedback should not escape MiniMessage tags: " + serialized);
        assertTrue(serialized.contains("<aqua>fresh herbs"),
                "Hint feedback should preserve inner aqua styling: " + serialized);

        helper.playHintFeedback(player, HintDefinition.DEFAULT.description());
        Component defaultSent = sent.get();
        assertNotNull(defaultSent);
        assertEquals("Culinary Clue: A culinary mystery waiting to be unraveled...", plain.serialize(defaultSent));
    }

    @Test
    void testPlayHintFeedbackWithoutMessageServiceSafelyRendersMiniMessageAndFallback() {
        DiscoveryFeedbackHelper helper = new DiscoveryFeedbackHelper();
        AtomicReference<Component> sent = new AtomicReference<>();
        Player player = mockPlayer(sent);

        helper.playHintFeedback(player, "Simmer <yellow>golden carrots</yellow>");
        Component parsed = sent.get();
        assertNotNull(parsed);
        assertEquals("Culinary Clue: Simmer golden carrots", plain.serialize(parsed));
        String serialized = miniMessage.serialize(parsed);
        assertFalse(serialized.contains("\\<yellow>"), "Null-service fallback should parse MiniMessage tags: " + serialized);
        assertTrue(serialized.contains("<yellow>golden carrots"), "Null-service fallback should preserve yellow styling: " + serialized);

        helper.playHintFeedback(player, null);
        Component fallback = sent.get();
        assertNotNull(fallback);
        assertEquals("Culinary Clue: You noticed something curious...", plain.serialize(fallback));
    }
}
