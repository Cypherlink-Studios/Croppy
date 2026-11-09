package dev.darkblade.croppy.platform.paper.i18n;

import dev.darkblade.croppy.api.i18n.LocaleRef;
import dev.darkblade.croppy.api.i18n.MessageKey;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.core.i18n.DefaultMessageCatalog;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Paper adapter service responsible for resolving localized message templates,
 * sanitizing arguments, and delivering Adventure Components to players and audiences.
 */
public class PaperMessageService {

    private static final MessageKey PREFIX_KEY = MessageKey.of("general.prefix");

    private final DefaultMessageCatalog catalog;
    private final MiniMessage miniMessage;
    private final Logger logger;

    public PaperMessageService(DefaultMessageCatalog catalog, Logger logger) {
        this.catalog = Objects.requireNonNull(catalog, "catalog cannot be null");
        this.miniMessage = MiniMessage.miniMessage();
        this.logger = logger;
    }

    public DefaultMessageCatalog getCatalog() {
        return catalog;
    }

    public MiniMessage getMiniMessage() {
        return miniMessage;
    }

    /**
     * Resolves the LocaleRef appropriate for the given audience.
     */
    public LocaleRef resolveLocale(Audience audience) {
        if (audience instanceof Player player) {
            return LocaleRef.from(player.locale());
        }
        return catalog.getDefaultLocale();
    }

    /**
     * Renders a message key to an Adventure Component for the specified audience.
     */
    public Component render(Audience audience, MessageKey key, TagResolver... resolvers) {
        LocaleRef locale = resolveLocale(audience);
        return render(locale, key, resolvers);
    }

    /**
     * Renders a message key to an Adventure Component for the specified audience with raw text replacements.
     */
    public Component renderWithReplacements(Audience audience, MessageKey key, java.util.Map<String, String> rawReplacements, TagResolver... resolvers) {
        LocaleRef locale = resolveLocale(audience);
        return renderWithReplacements(locale, key, rawReplacements, resolvers);
    }

    /**
     * Renders a message key to an Adventure Component for an explicit target locale.
     */
    public Component render(LocaleRef targetLocale, MessageKey key, TagResolver... resolvers) {
        return renderWithReplacements(targetLocale, key, java.util.Map.of(), resolvers);
    }

    /**
     * Renders a message key to an Adventure Component for an explicit target locale with raw string replacements.
     */
    public Component renderWithReplacements(LocaleRef targetLocale, MessageKey key, java.util.Map<String, String> rawReplacements, TagResolver... resolvers) {
        Objects.requireNonNull(key, "key cannot be null");
        LocaleRef locale = (targetLocale != null) ? targetLocale : catalog.getDefaultLocale();

        Optional<String> templateOpt = catalog.getTemplate(locale, key);
        if (templateOpt.isEmpty()) {
            if (logger != null && catalog.markMissingKeyReported(key.asString())) {
                logger.warning("Missing translation key '" + key.asString() + "' for locale [" + locale + "].");
            }
            return miniMessage.deserialize("<red><missing:" + key.asString() + "></red>");
        }

        String template = templateOpt.get();
        if (rawReplacements != null && !rawReplacements.isEmpty()) {
            for (java.util.Map.Entry<String, String> entry : rawReplacements.entrySet()) {
                template = template.replace(entry.getKey(), entry.getValue());
            }
        }

        if (resolvers == null || resolvers.length == 0) {
            return miniMessage.deserialize(template);
        }
        return miniMessage.deserialize(template, TagResolver.resolver(resolvers));
    }

    /**
     * Sends a localized message without any prefix.
     */
    public void sendMessage(Audience audience, MessageKey key, TagResolver... resolvers) {
        if (audience == null) return;
        audience.sendMessage(render(audience, key, resolvers));
    }

    /**
     * Sends a localized message without any prefix with raw text replacements.
     */
    public void sendMessage(Audience audience, MessageKey key, Map<String, String> rawReplacements, TagResolver... resolvers) {
        if (audience == null) return;
        audience.sendMessage(renderWithReplacements(audience, key, rawReplacements, resolvers));
    }

    /**
     * Sends a localized message prepended with the localized prefix.
     */
    public void sendPrefixedMessage(Audience audience, MessageKey key, TagResolver... resolvers) {
        if (audience == null) return;
        Component message = render(audience, key, resolvers);
        Optional<String> prefixTemplate = catalog.getTemplate(resolveLocale(audience), PREFIX_KEY);
        if (prefixTemplate.isPresent() && !prefixTemplate.get().isBlank()) {
            Component prefix = miniMessage.deserialize(prefixTemplate.get());
            audience.sendMessage(prefix.append(message));
        } else {
            audience.sendMessage(message);
        }
    }

    /**
     * Sends a localized message prepended with the localized prefix with raw text replacements.
     */
    public void sendPrefixedMessage(Audience audience, MessageKey key, Map<String, String> rawReplacements, TagResolver... resolvers) {
        if (audience == null) return;
        Component message = renderWithReplacements(audience, key, rawReplacements, resolvers);
        Optional<String> prefixTemplate = catalog.getTemplate(resolveLocale(audience), PREFIX_KEY);
        if (prefixTemplate.isPresent() && !prefixTemplate.get().isBlank()) {
            Component prefix = miniMessage.deserialize(prefixTemplate.get());
            audience.sendMessage(prefix.append(message));
        } else {
            audience.sendMessage(message);
        }
    }

    /**
     * Sends a localized message to a player's action bar.
     */
    public void sendActionBar(Player player, MessageKey key, TagResolver... resolvers) {
        if (player == null) return;
        player.sendActionBar(render(player, key, resolvers));
    }

    /**
     * Sends a localized message to a player's action bar with raw text replacements.
     */
    public void sendActionBarWithReplacements(Player player, MessageKey key, Map<String, String> rawReplacements, TagResolver... resolvers) {
        if (player == null) return;
        player.sendActionBar(renderWithReplacements(player, key, rawReplacements, resolvers));
    }

    /**
     * Convenience method to render a message key by string name with alternating key-value arguments.
     */
    public Component render(Audience audience, String key, Object... args) {
        return renderWithReplacements(resolveLocale(audience), MessageKey.of(key), rawArgs(args), args(args));
    }

    /**
     * Convenience method to send a prefixed message by string name with alternating key-value arguments.
     */
    public void sendPrefixedMessage(Audience audience, String key, Object... args) {
        sendPrefixedMessage(audience, MessageKey.of(key), rawArgs(args), args(args));
    }

    /**
     * Convenience method to send an unprefixed message by string name with alternating key-value arguments.
     */
    public void sendMessage(Audience audience, String key, Object... args) {
        sendMessage(audience, MessageKey.of(key), rawArgs(args), args(args));
    }

    /**
     * Convenience method to send an action bar message by string name with alternating key-value arguments.
     */
    public void sendActionBar(Player player, String key, Object... args) {
        sendActionBarWithReplacements(player, MessageKey.of(key), rawArgs(args), args(args));
    }

    // ---------------------------------------------------------------------------------------------
    // Content Localization Overrides
    // ---------------------------------------------------------------------------------------------

    /**
     * Resolves the localized display name for an item definition, falling back to definition name.
     */
    public Component resolveItemDisplayName(Audience audience, ItemDefinition def) {
        LocaleRef locale = resolveLocale(audience);
        MessageKey overrideKey = MessageKey.of("content.item." + def.key().asString().replace(':', '.') + ".name");
        Optional<String> template = catalog.getTemplate(locale, overrideKey);
        if (template.isPresent()) {
            return miniMessage.deserialize(template.get());
        }
        return miniMessage.deserialize(def.displayName());
    }

    /**
     * Resolves localized lore lines for an item definition, falling back to definition lore.
     */
    public List<Component> resolveItemLore(Audience audience, ItemDefinition def) {
        LocaleRef locale = resolveLocale(audience);
        MessageKey overrideKey = MessageKey.of("content.item." + def.key().asString().replace(':', '.') + ".lore");
        Optional<String> template = catalog.getTemplate(locale, overrideKey);
        if (template.isPresent()) {
            String[] lines = template.get().split("\n");
            List<Component> result = new ArrayList<>(lines.length);
            for (String line : lines) {
                result.add(miniMessage.deserialize(line));
            }
            return result;
        }

        List<Component> result = new ArrayList<>(def.lore().size());
        for (String line : def.lore()) {
            result.add(miniMessage.deserialize(line));
        }
        return result;
    }

    /**
     * Resolves the localized display name for a crop species, falling back to species definition name.
     */
    public Component resolveSpeciesDisplayName(Audience audience, Species species) {
        if (species == null) {
            return Component.empty();
        }
        LocaleRef locale = resolveLocale(audience);
        MessageKey overrideKey = MessageKey.of("content.species." + species.getKey().asString().replace(':', '.') + ".name");
        Optional<String> template = catalog.getTemplate(locale, overrideKey);
        if (template.isPresent()) {
            return miniMessage.deserialize(template.get());
        }
        String name = (species.getName() != null) ? species.getName() : species.getKey().asString();
        return miniMessage.deserialize(name);
    }

    /**
     * Resolves the localized display name for a recipe definition, falling back to definition name.
     */
    public Component resolveRecipeDisplayName(Audience audience, RecipeDefinition recipe) {
        if (recipe == null) {
            return Component.empty();
        }
        LocaleRef locale = resolveLocale(audience);
        MessageKey overrideKey = MessageKey.of("content.recipe." + recipe.key().asString().replace(':', '.') + ".name");
        Optional<String> template = catalog.getTemplate(locale, overrideKey);
        if (template.isPresent()) {
            return miniMessage.deserialize(template.get());
        }
        return miniMessage.deserialize(recipe.displayName());
    }

    // ---------------------------------------------------------------------------------------------
    // Argument Sanitization & Helpers
    // ---------------------------------------------------------------------------------------------

    /**
     * Extracts raw `{key}` placeholders for string substitutions inside MiniMessage tag attributes.
     */
    public static Map<String, String> rawArgs(Object... keyValues) {
        if (keyValues == null || keyValues.length < 2) {
            return Map.of();
        }
        Map<String, String> map = new HashMap<>();
        int count = keyValues.length / 2;
        for (int i = 0; i < count; i++) {
            String k = String.valueOf(keyValues[i * 2]);
            Object v = keyValues[i * 2 + 1];
            if (v instanceof String || v instanceof Number || v instanceof Boolean || v instanceof Key) {
                map.put("{" + k + "}", String.valueOf(v));
            }
        }
        return map;
    }

    /**
     * Creates a TagResolver that safely injects an unparsed, sanitized value, or preserves rich components, colors, and tags.
     */
    public static TagResolver arg(String key, Object value) {
        if (value instanceof TagResolver resolver) {
            return resolver;
        }
        if (value instanceof Tag tag) {
            return TagResolver.resolver(key, tag);
        }
        if (value instanceof TextColor color) {
            return TagResolver.resolver(key, Tag.styling(color));
        }
        if (value instanceof Component component) {
            return Placeholder.component(key, component);
        }
        return Placeholder.unparsed(key, (value != null) ? value.toString() : "null");
    }

    /**
     * Creates a TagResolver that parses a MiniMessage string argument.
     * Use with caution and only for trusted content.
     */
    public static TagResolver parsed(String key, String miniMessageString) {
        return Placeholder.parsed(key, miniMessageString != null ? miniMessageString : "");
    }

    /**
     * Creates TagResolvers from alternating key-value pairs with automatic argument sanitization.
     * Example: args("player", "Steve", "amount", 5)
     */
    public static TagResolver[] args(Object... keyValues) {
        if (keyValues == null || keyValues.length < 2) {
            return new TagResolver[0];
        }
        int count = keyValues.length / 2;
        TagResolver[] resolvers = new TagResolver[count];
        for (int i = 0; i < count; i++) {
            String k = String.valueOf(keyValues[i * 2]);
            Object v = keyValues[i * 2 + 1];
            resolvers[i] = arg(k, v);
        }
        return resolvers;
    }
}
